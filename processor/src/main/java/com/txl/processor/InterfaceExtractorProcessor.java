package com.txl.processor;

import com.txl.annotations.InterfaceExtractor;

import java.io.IOException;
import java.io.Writer;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.Elements;
import javax.tools.JavaFileObject;

public class InterfaceExtractorProcessor extends AbstractProcessor {

    private Filer mFiler;   // 注解处理器创建文件的File工具
    private Elements mElementUtils; // 相关元素处理工具

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        mFiler = processingEnv.getFiler();
        mElementUtils = processingEnv.getElementUtils();
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        Set<String> annotationsType = new LinkedHashSet<>();
        annotationsType.add(InterfaceExtractor.class.getCanonicalName());
        return annotationsType;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.RELEASE_7;
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        // 获取所有使用该注解的元素
        Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith(InterfaceExtractor.class);
        for (Element element : elements) {
            TypeElement typeElement = (TypeElement) element;
            PackageElement packageElement = mElementUtils.getPackageOf(typeElement);
            InterfaceExtractor annotation = typeElement.getAnnotation(InterfaceExtractor.class);
            String packageName = packageElement.getQualifiedName().toString();
            String className = annotation.value();
            JavaFileObject sourceFile;
            try {
                sourceFile = mFiler.createSourceFile(packageName + "." + className, typeElement);
                Writer writer = sourceFile.openWriter();
                writer.write(generateJavaCode(packageName, className, typeElement));
                writer.flush();
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return true;
    }

    private String generateJavaCode(String packageName, String className, TypeElement typeElement) {
        StringBuilder builder = new StringBuilder();
        builder.append("// Generated code. Do not modify!\n");
        builder.append("package ").append(packageName).append(";\n\n");
        builder.append('\n');
        builder.append("public interface ").append(className).append(" {\n\n");
        List<? extends Element> executableElements = typeElement.getEnclosedElements();
        for (Element element : executableElements) {
            if (element instanceof ExecutableElement) {
                ExecutableElement executableElement = (ExecutableElement) element;
                if (executableElement.getModifiers().contains(Modifier.PUBLIC) &&
                        !(executableElement.getModifiers().contains(Modifier.STATIC)) &&
                        executableElement.getKind() != ElementKind.CONSTRUCTOR) {
                    builder.append("public ");
                    builder.append(executableElement.getReturnType());
                    builder.append(" ").append(executableElement.getSimpleName()).append(" (");
                    List<? extends VariableElement> variableElements = executableElement.getParameters();
                    int i = 0;
                    for (VariableElement variableElement : variableElements) {
                        builder.append(variableElement.asType()).append(" ").append(variableElement.getSimpleName());
                        if (++i < variableElements.size()) {
                            builder.append(", ");
                        }
                    }
                    builder.append(");\n\n");
                }
            }
        }
        builder.append("}\n");
        return builder.toString();
    }
}
