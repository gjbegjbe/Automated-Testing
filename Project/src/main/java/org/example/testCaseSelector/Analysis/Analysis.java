package org.example.testCaseSelector.Analysis;

import com.ibm.wala.classLoader.Language;
import com.ibm.wala.classLoader.ShrikeBTMethod;
import com.ibm.wala.ipa.callgraph.*;
import com.ibm.wala.ipa.callgraph.impl.AllApplicationEntrypoints;
import com.ibm.wala.ipa.callgraph.impl.Util;
import com.ibm.wala.ipa.callgraph.propagation.SSAPropagationCallGraphBuilder;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.ClassHierarchyFactory;
import com.ibm.wala.shrikeCT.InvalidClassFileException;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.annotations.Annotation;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.config.AnalysisScopeReader;
import org.example.testCaseSelector.Main;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Scanner;

public class Analysis {
    private String[] args;
    // 项目名称
    private static String projectName;
    // 调用图
    private static CallGraph callGraph;
    // 变更信息
    private static HashSet<String> changes = new HashSet<>();

    private static AnalysisScope analysisScope;

    private static ClassHierarchy classHierarchy;

    private static AllApplicationEntrypoints entrypoints;

    private static AnalysisOptions option;

    private static SSAPropagationCallGraphBuilder builder;

    private static String projectTarget;

    private static String changeInfo;

    private static String[] targetPath;




    // test类和其@Test方法
    private static HashMap<String, HashSet<String>> testMethodsOfTestClasses = new HashMap<>();

    public static CallGraph getCallGraph() {
        return callGraph;
    }

    public static String getProjectName() {
        return projectName;
    }

    public static HashSet<String> getChanges() {
        return changes;
    }

    public static HashMap<String, HashSet<String>> getTestMethodsOfTestClasses() {
        return testMethodsOfTestClasses;
    }

    public Analysis(String[] args) {
        this.args = args;
    }

    /*
            生成0-CFA调用图
            获取变更信息
            获取测试类和其@Test方法
            */
    public void analyze() throws IOException, InvalidClassFileException, ClassHierarchyException, CancelException {
        projectTarget = args[1];
        changeInfo = args[2];
        targetPath = args[1].split("\\\\");

        try {
            projectName = targetPath[targetPath.length - 2].split("-")[1];//判断路径是否合法
        } catch (Exception e) {
        }
        if (!projectTarget.endsWith(File.separator))
            projectTarget += File.separator;



        // 读取变更信息
        getChanges(changeInfo);

        // 读取配置文件
        analysisScope = AnalysisScopeReader.readJavaScope("scope.txt",
                new File("exclusion.txt"),
                Main.class.getClassLoader());

        //添加测试类
        addTestClass();
        //添加src类
        addSrcClass();
    }

    // 读取变更信息
    private static void getChanges(String path) {
        try {
            Scanner in = new Scanner(new FileReader(path));
            while (in.hasNextLine())
                changes.add(in.nextLine());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //递归添加目录下的class文件
    private static void addClassFiles(AnalysisScope scope, File parentDir) throws InvalidClassFileException {
        File[] fileList = parentDir.listFiles();
        for (File file : fileList) {
            if (file.isFile() && file.getAbsolutePath().endsWith(".class")) {
                scope.addClassFileToScope(ClassLoaderReference.Application, file);
            } else if (file.isDirectory()) {
                addClassFiles(scope, file);
            }
        }
    }

    //添加测试类
    private static void addTestClass() throws InvalidClassFileException, ClassHierarchyException, CancelException {
        addClassFiles(analysisScope, new File(projectTarget + "test-classes"));
        classHierarchy = ClassHierarchyFactory.makeWithRoot(analysisScope);
        entrypoints = new AllApplicationEntrypoints(analysisScope, classHierarchy);
        option = new AnalysisOptions(analysisScope, entrypoints);
        builder = Util.makeZeroCFABuilder(
                Language.JAVA, option, new AnalysisCacheImpl(), classHierarchy, analysisScope);
        callGraph = builder.makeCallGraph(option);
        // 遍历cg中所有的节点，获取测试类名和测试类的@Test方法
        for (CGNode node : callGraph) {
            if (node.getMethod() instanceof ShrikeBTMethod) {
                ShrikeBTMethod method = (ShrikeBTMethod) node.getMethod();
                // 使用Primordial类加载器加载的类都属于Java原生类，一般不关心
                if ("Application".equals(method.getDeclaringClass().getClassLoader().toString())) {
                    String classInnerName = method.getDeclaringClass().getName().toString();
                    String signature = method.getSignature();
                    // 放入测试类
                    if (!testMethodsOfTestClasses.containsKey(classInnerName)) {
                        testMethodsOfTestClasses.put(classInnerName, new HashSet<>());
                    }
                    // 如果是junit的@Test方法
                    Collection<Annotation> annotations = method.getAnnotations();
                    for (Annotation annotation : annotations) {
                        if (annotation.toString().contains("Lorg/junit/Test")) {
                            testMethodsOfTestClasses.get(classInnerName).add(signature);
                            break;
                        }
                    }
                }
            }
        }



    }

    //添加src类
    private static void addSrcClass() throws InvalidClassFileException, CancelException, ClassHierarchyException {
        addClassFiles(analysisScope, new File(projectTarget + "classes"));
        classHierarchy = ClassHierarchyFactory.makeWithRoot(analysisScope);
        entrypoints = new AllApplicationEntrypoints(analysisScope, classHierarchy);
        option = new AnalysisOptions(analysisScope, entrypoints);
        builder = Util.makeZeroCFABuilder(
                Language.JAVA, option, new AnalysisCacheImpl(), classHierarchy, analysisScope);
        callGraph = builder.makeCallGraph(option);
    }


}
