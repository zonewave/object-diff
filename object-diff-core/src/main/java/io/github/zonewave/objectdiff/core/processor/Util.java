package io.github.zonewave.objectdiff.core.processor;

public class Util {

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
    protected static String getterCoreName(final String name) {
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
