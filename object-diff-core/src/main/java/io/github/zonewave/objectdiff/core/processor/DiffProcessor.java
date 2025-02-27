package io.github.zonewave.objectdiff.core.processor;


import io.github.zonewave.objectdiff.core.common.Change;
import io.github.zonewave.objectdiff.core.ifaces.Diff;
import com.google.auto.service.AutoService;
import com.palantir.javapoet.CodeBlock;
import com.palantir.javapoet.JavaFile;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.MethodSpec.Builder;
import com.palantir.javapoet.ParameterizedTypeName;
import com.palantir.javapoet.TypeName;
import com.palantir.javapoet.TypeSpec;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;


@AutoService(Processor.class)
@SupportedAnnotationTypes("io.github.zonewave.objectdiff.core.ifaces.Diff")
@SupportedSourceVersion(SourceVersion.RELEASE_21)
public class DiffProcessor extends AbstractProcessor {

    private static final String ImplSuffix = "Impl";

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {

        for (Element element : roundEnv.getElementsAnnotatedWith(Diff.class)) {
            if (!isSupportAnnotations(element)) {
                processingEnv.getMessager().printError("not in abstract class or interface");
                return false;
            }

            PackageElement packageElement = processingEnv.getElementUtils().getPackageOf(element);
            String packageName = packageElement.getQualifiedName().toString();
            String className = element.getSimpleName() + ImplSuffix;

            // create class builder
            TypeSpec.Builder diffClassBuilder = TypeSpec.classBuilder(className).addSuperinterface(element.asType())
                    .addModifiers(Modifier.PUBLIC);

            for (Element enclosedElement : element.getEnclosedElements()) {
                if (enclosedElement.getKind() == ElementKind.METHOD) {
                    var methodName = enclosedElement.getSimpleName().toString();
                    var executableElement = (ExecutableElement) enclosedElement;
                    var typeNode = executableElement.getParameters().getFirst().asType();

                    var parameterTypeElement = (TypeElement) processingEnv.getTypeUtils().asElement(typeNode);
                    // diff method
                    MethodSpec diffMethod = createDiffMethod(parameterTypeElement, methodName);

                    diffClassBuilder.addMethod(diffMethod);
                }
            }

            // create class
            TypeSpec diffClass = diffClassBuilder.build();
            // create file
            JavaFile javaFile = JavaFile.builder(packageName, diffClass).build();

            try {
                javaFile.writeTo(processingEnv.getFiler());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return true;
    }

    private final TypeName mapOfChanges = ParameterizedTypeName.get(Map.class, String.class, Change.class);

    private boolean isSupportAnnotations(Element element) {
        if (element.getKind().isClass() && element.getModifiers().contains(Modifier.ABSTRACT)) {
            return true;
        }
        return element.getKind().isInterface();
    }

    private MethodSpec createDiffMethod(TypeElement className, String methodName) {
        var classTypeName = TypeName.get(className.asType());
        var builder = MethodSpec.methodBuilder(methodName).addModifiers(Modifier.PUBLIC).addAnnotation(Override.class)
                .returns(mapOfChanges).addParameter(classTypeName, "oldObj").addParameter(classTypeName, "newObj");
        return generateMethodBody(builder, className).build();
    }

    private Builder generateMethodBody(Builder builder, TypeElement typeData) {
        builder.addStatement("$T changeFields = new $T<>() ", mapOfChanges, HashMap.class);
        var methodMap = getMethodMap(typeData);
        for (var f : typeData.getEnclosedElements()) {
            if (f.getKind() != ElementKind.FIELD) {
                continue;
            }
            var fieldElement = (VariableElement) f;
            var fieldName = fieldElement.getSimpleName().toString();
            var fieldKind = fieldElement.asType().getKind();
            var getName = getterMethodName(fieldElement, methodMap);
            if (Objects.equals(getName, "")) {
                continue;
            }
            var putMapCodeBlock = CodeBlock.builder()
                    .add("changeFields.put($1S, new $2T(oldObj.$3L, newObj.$3L))", fieldName, Change.class, getName)
                    .build();
            if (fieldKind.isPrimitive()) {
                builder.beginControlFlow("if (oldObj.$1L != newObj.$1L) ", getName);
            } else {
                builder.beginControlFlow("if (oldObj.$1L == null && newObj.$1L !=null) ", getName);
                builder.addStatement(putMapCodeBlock);
                builder.nextControlFlow("else if (oldObj.$1L != null &&  !oldObj.$1L.equals(newObj.$1L)) ", getName);
            }
            builder.addStatement(putMapCodeBlock);
            builder.endControlFlow();
        }

        builder.addStatement("return changeFields");

        return builder;

    }

    private Map<String, ExecutableElement> getMethodMap(TypeElement typeElement) {
        return typeElement.getEnclosedElements().stream()
                .filter(f -> f.getKind() == ElementKind.METHOD && f.getModifiers().contains(Modifier.PUBLIC))
                .collect(Collectors.toMap(f -> f.getSimpleName().toString(), f -> (ExecutableElement) f));

    }


    private String getterMethodName(VariableElement variableElement, Map<String, ExecutableElement> methodMap) {
        var fieldName = variableElement.getSimpleName().toString();
        var fieldKind = variableElement.asType().getKind();
        var getNameSuffix = getterCoreName(fieldName);
        if (methodMap.containsKey("get" + getNameSuffix)) {
            return "get" + getNameSuffix + "()";
        } else if (fieldKind == TypeKind.BOOLEAN && methodMap.containsKey("is" + getNameSuffix)) {
            return "is" + getNameSuffix + "()";
        } else if (variableElement.getModifiers().contains(Modifier.PUBLIC)) {
            return fieldName;
        } else {
            return "";
        }

    }

    /**
     * Get the name of the body of the getter method for field name
     * This method aims to convert a field name into the corresponding getter method name according to Java naming
     * conventions
     * It does not handle the addition of the "get" or "is" prefix, only the processing of the field name part
     * CN: 根据字段名获取 对应getter 方法的 body name ，根据 Java 命名约定将一个字段名转换为对应的 getter 方法名 它不处理 “get” 或 “is” 前缀的添加，只处理字段名部分的处理
     *
     * @param name The field name, cannot be null
     * @return The processed string, suitable for use as the body of a getter method name
     */
    public static String getterCoreName(String name) {
        // If the field name is null or empty, return the original value directly
        if (name == null || name.isEmpty()) {
            return name;
        }
        // If the first character of the field name is lowercase and the second character is uppercase, return the original name directly
        if (name.length() > 1 && Character.isUpperCase(name.charAt(1)) && Character.isLowerCase(name.charAt(0))) {
            return name;
        }
        // If the field name starts with "is" and the third character is uppercase, return the substring after removing the "is" prefix
        if (name.length() > 2 && name.startsWith("is") && Character.isUpperCase(name.charAt(2))) {
            return name.substring(2);
        }
        // For other cases, capitalize the first character of the field name and keep the rest unchanged
        return name.substring(0, 1).toUpperCase() + name.substring(1);
    }
}
