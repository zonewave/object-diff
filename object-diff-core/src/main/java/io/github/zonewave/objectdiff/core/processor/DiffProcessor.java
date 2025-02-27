package io.github.zonewave.objectdiff.core.processor;


import static io.github.zonewave.objectdiff.core.processor.Util.getterCoreName;

import com.google.auto.service.AutoService;
import com.palantir.javapoet.CodeBlock;
import com.palantir.javapoet.JavaFile;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.MethodSpec.Builder;
import com.palantir.javapoet.ParameterizedTypeName;
import com.palantir.javapoet.TypeName;
import com.palantir.javapoet.TypeSpec;
import io.github.zonewave.objectdiff.core.common.Change;
import io.github.zonewave.objectdiff.core.ifaces.Diff;
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
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;


@AutoService(Processor.class)
@SupportedAnnotationTypes("io.github.zonewave.objectdiff.core.ifaces.Diff")
@SupportedSourceVersion(SourceVersion.RELEASE_21)
public class DiffProcessor extends AbstractProcessor {

    private static final String ImplSuffix = "Impl";
    private final TypeName mapOfChanges = ParameterizedTypeName.get(Map.class, String.class, Change.class);

    private static Map<String, ExecutableElement> getMethodMap(final TypeElement typeElement) {
        return typeElement.getEnclosedElements().stream()
                .filter(f -> f.getKind() == ElementKind.METHOD && f.getModifiers().contains(Modifier.PUBLIC))
                .collect(Collectors.toMap(f -> f.getSimpleName().toString(), f -> (ExecutableElement) f));

    }


    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {

        for (var element : roundEnv.getElementsAnnotatedWith(Diff.class)) {
            if (!isSupportAnnotations(element)) {
                processingEnv.getMessager().printError("not in abstract class or interface");
                return false;
            }

            var packageElement = processingEnv.getElementUtils().getPackageOf(element);
            var packageName = packageElement.getQualifiedName().toString();

            var diffClass = getDiffClass(element);
            // create file
            var javaFile = JavaFile.builder(packageName, diffClass).build();

            try {
                javaFile.writeTo(processingEnv.getFiler());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return true;
    }

    private TypeSpec getDiffClass(Element element) {
        var className = element.getSimpleName() + ImplSuffix;
        // create class builder
        var diffClassBuilder = TypeSpec.classBuilder(className).addSuperinterface(element.asType())
                .addModifiers(Modifier.PUBLIC);

        for (var eElem : element.getEnclosedElements()) {
            if (eElem.getKind() == ElementKind.METHOD && eElem instanceof ExecutableElement execElem) {
                var methodName = execElem.getSimpleName().toString();
                var typeNode = execElem.getParameters().getFirst().asType();

                var parameterTypeElement = (TypeElement) processingEnv.getTypeUtils().asElement(typeNode);
                // diff method
                diffClassBuilder.addMethod(createDiffMethod(parameterTypeElement, methodName));
            }
        }
        // create class
        return diffClassBuilder.build();
    }

    private boolean isSupportAnnotations(final Element element) {
        if (element.getKind().isClass() && element.getModifiers().contains(Modifier.ABSTRACT)) {
            return true;
        }
        return element.getKind().isInterface();
    }

    private MethodSpec createDiffMethod(final TypeElement className, final String methodName) {
        var classTypeName = TypeName.get(className.asType());
        var builder = MethodSpec.methodBuilder(methodName).addModifiers(Modifier.PUBLIC).addAnnotation(Override.class)
                .returns(mapOfChanges).addParameter(classTypeName, "oldObj").addParameter(classTypeName, "newObj");
        return generateMethodBody(builder, className).build();
    }

    private Builder generateMethodBody(Builder builder, final TypeElement typeData) {
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

    private String getterMethodName(final VariableElement variableElement,
            final Map<String, ExecutableElement> methodMap) {
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
}
