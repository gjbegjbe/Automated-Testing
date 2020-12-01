package org.example.testCaseSelector;

import java.io.*;
import java.util.*;

import com.ibm.wala.classLoader.ShrikeBTMethod;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.cha.CHACallGraph;
import com.ibm.wala.ipa.callgraph.impl.AllApplicationEntrypoints;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyFactory;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.util.config.AnalysisScopeReader;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.shrikeCT.InvalidClassFileException;
import com.ibm.wala.types.annotations.Annotation;
import com.ibm.wala.util.CancelException;

public class Main {
    private static HashSet<String> testMethodsSignatures = new HashSet<String>();//所有签名
    private static HashSet<String> allClasses = new HashSet<String>();  //所有类
    private static HashSet<String> testMethods = new HashSet<String>(); //所有测试方法
    private static HashSet<String> allMethods = new HashSet<String>();  //所有方法
    private static HashSet<String> allClassesOfMethods = new HashSet<String>();//所有方法所属类
    private static HashSet<String> allCallee = new HashSet<String>();//所有被调用
    private static HashMap<String, List<String>> callRelation = new HashMap<String, List<String>>(); // key为被调用，value为调用
    private static ArrayList<String> changeInfoList = new ArrayList<String>(); // 变更信息列表
    public static HashMap<String, List<String>> classDependencies = new HashMap<>();  // 类依赖
    public static HashMap<String, List<String>> methodDependencies = new HashMap<>(); //方法依赖
    public static HashSet<String> relatedClasses = new HashSet<String>();//所有依赖被搜索类的类
    public static HashSet<String> relatedMethods = new HashSet<String>();//所有依赖被搜索方法的方法
    public static HashSet<String> selectedMethods = new HashSet<String>();//选择的方法

    public static ClassHierarchy cha;
    public static CHACallGraph cg;
    public static AnalysisScope scope;
    private static String commandType;
    private static String projectTarget;
    private static String changeInfo;
    public static File[] classFiles = new File[0];
    public static File[] testClassFiles = new File[0];


    public static void main(String[] args) throws IOException, ClassHierarchyException, InvalidClassFileException, CancelException {

        commandType=args[0];
        projectTarget=args[1];
        changeInfo=args[2];

        if ((!commandType.equals("-c")) && (!commandType.equals("-m"))) {
            System.out.println("wrong command");
            return;
        }

        //添加class文件
        addClass();

        //生成分析域
        makeScope();

        //将类加入到分析域中
        addClassToScope();

        //生成类关系层次对象并构建调用图
        makeClassHierarchy();

        //获取所有分析相关的类
        getAllClasses();

        //生成调用关系
        buildRelations();

        if (commandType == "-c") {
            //获取类的依赖并且保存
            getClassDependencies();

            //生成类的dot文件
            writeDotFile("c");
        } else {
            //获取方法的依赖并且保存
            getMethodDependencies();

            //生成方法的dot文件
            writeDotFile("m");
        }

        //根据changeInfo.txt获取更改信息
        getChangeInfo();

        //选择测试方法
        select();

        //保存结果
        saveTxt();
    }

    //添加class文件
    public static void addClass() {
        File projectTargetFile = new File(projectTarget);
        File[] projectTargetFiles = projectTargetFile.listFiles();

        //寻找classes和test-classes文件夹，因为只有这两个里面有.classes文件
        for (File f : projectTargetFiles) {
            if ("classes".equals(f.getName())) {
                String classFilePath = f.getAbsolutePath() + "\\net\\mooctest";
                File classFile = new File(classFilePath);
                classFiles = classFile.listFiles();
            } else if ("test-classes".equals(f.getName())) {
                String testClassFilePath = f.getAbsolutePath() + "\\net\\mooctest";
                File testClassFile = new File(testClassFilePath);
                testClassFiles = testClassFile.listFiles();

            }
        }
    }

    //生成分析域
    public static void makeScope() throws IOException {
        scope = AnalysisScopeReader.readJavaScope(
                "scope.txt",
                new File("exclusion.txt"),
                ClassLoader.getSystemClassLoader());
    }

    //将类加入到分析域中
    public static void addClassToScope() {
        try {
            for (File f : classFiles) {
                scope.addClassFileToScope(ClassLoaderReference.Application, f);
            }
        } catch (Exception e) {
            System.out.println("Empty!");
        }

        try {
            for (File f : testClassFiles) {
                scope.addClassFileToScope(ClassLoaderReference.Application, f);
            }
        } catch (Exception e) {
            System.out.println("Empty!");
        }
    }

    //生成类关系层次对象并构建调用图
    public static void makeClassHierarchy() throws ClassHierarchyException, CancelException {
        //生成类层次关系对象
        cha = ClassHierarchyFactory.makeWithRoot(scope);
        //构建调用图
        cg = new CHACallGraph(cha);
        cg.init(new AllApplicationEntrypoints(scope, cha));
    }

    //获取所有分析相关的类
    public static void getAllClasses() {
        //遍历cg中所有节点
        for (CGNode node : cg) {
            if (node.getMethod() instanceof ShrikeBTMethod) {
                ShrikeBTMethod method = (ShrikeBTMethod) node.getMethod();
                if ("Application".equals(method.getDeclaringClass().getClassLoader().toString())) {
                    //获取方法的类
                    String classInnerName = method.getDeclaringClass().getName().toString();
                    allClasses.add(classInnerName);
                    //获取方法签名
                    String signature = method.getSignature();
                    testMethodsSignatures.add(signature);
                }
            }
        }
    }

    //生成调用关系
    public static void buildRelations() throws InvalidClassFileException {
        for (CGNode node : cg) {
            if (node.getMethod() instanceof ShrikeBTMethod) {
                ShrikeBTMethod method = (ShrikeBTMethod) node.getMethod();
                if ("Application".equals(method.getDeclaringClass().getClassLoader().toString())) {
                    String classInnerName = method.getDeclaringClass().getName().toString();
                    String signature = method.getSignature();
                    String caller = classInnerName + " " + signature;

                    //如果注释符合，就是一个测试方法
                    for (Annotation annotationType : method.getAnnotations()) {
                        if (annotationType.toString().equals("Annotation type <Application,Lorg/junit/Test> {timeout=4000}")) {
                            testMethods.add(caller);
                            break;
                        }
                    }

                    //获取调用信息
                    Collection<CallSiteReference> references = method.getCallSites();
                    for (CallSiteReference reference : references) {
                        //类名
                        String calledClass = reference.getDeclaredTarget().toString().replace(" ", "").split(",")[1].split("\\$")[0];
                        //方法名
                        String calledMethod = reference.getDeclaredTarget().getSignature();

                        String callee = calledClass + " " + calledMethod;

                        MethodReference methodReference = reference.getDeclaredTarget();
                        String refSignature = methodReference.getSignature();

                        //去除java和org开头的
                        if (!refSignature.startsWith("java") && !refSignature.startsWith("org")) {
                            //将被调用和调用者写入调用关系列表
                            if (callRelation.containsKey(callee)) {
                                if (!callRelation.get(callee).contains(caller)) {
                                    callRelation.get(callee).add(caller);
                                }
                            }
                            //如果没有这个callee，就新建一个
                            else {
                                List<String> list = new ArrayList<String>();
                                list.add(caller);
                                callRelation.put(callee, list);
                            }
                            //加入到所有方法中
                            if (!allMethods.contains(callee)) {
                                allMethods.add(callee);
                            }
                            if (!allMethods.contains(caller)) {
                                allMethods.add(caller);
                            }

                        }
                    }

                    //加入被调用的类，这是在-c下使用的
                    for (String callee : callRelation.keySet()) {
                        //提取前面的类名，加入到allClassesOfMethods中
                        String clazz = callee.toString().split(" ")[0];
                        if (!(allClassesOfMethods.contains(clazz))) {
                            allClassesOfMethods.add(clazz);
                        }
                        for (String c : callRelation.get(callee)) {
                            allClassesOfMethods.add(c.split(" ")[0]);
                        }
                        allCallee.add(callee.split(" ")[0]);
                    }
                }
            }
        }
    }

    //获取类的依赖并且保存
    public static void getClassDependencies() {
        for (String s : callRelation.keySet()) {
            //被调用名
            String callee = s.split(" ")[0];
            if (allClasses.contains(callee)) {
                //如果没有这个callee，就初始化
                if (!classDependencies.containsKey(callee)) {
                    List<String> list = new ArrayList<>();
                    classDependencies.put(callee, list);
                }
                //加入到类的依赖中
                for (String string : callRelation.get(s)) {
                    String caller = string.split(" ")[0];
                    if ((!classDependencies.get(callee).contains(caller)) && (allClasses.contains(caller))) {
                        classDependencies.get(callee).add(caller);
                    }
                }
            }
        }
    }

    //获取方法的依赖并且保存
    public static void getMethodDependencies() {
        for (String s : callRelation.keySet()) {
            String callee = s.split(" ")[1];
            if (allClasses.contains(s.split(" ")[0])) {
                //初始化没有出现过的类
                if (!methodDependencies.containsKey(callee)) {
                    List<String> list = new ArrayList<>();
                    methodDependencies.put(callee, list);
                }
                //加入到方法的依赖中
                for (String string : callRelation.get(s)) {
                    String caller = string.split(" ")[1];
                    if ((!methodDependencies.get(callee).contains(caller)) && (allClasses.contains(string.split(" ")[0]))) {
                        methodDependencies.get(callee).add(caller);
                    }
                }
            }
        }
    }

    //生成dot文件
    public static void writeDotFile(String commandType1) {
        try {
            File file = new File(commandType1 == "c" ? "class.dot" : "method.dot");
            FileWriter fileWriter = new FileWriter(file);
            BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
            bufferedWriter.write("digraph dependencies {\n");
            if (commandType1 == "c") {
                for (String string : classDependencies.keySet()) {
                    for (String s : classDependencies.get(string)) {
                        bufferedWriter.write("\t" + "\"" + string + "\" -> \"" + s + "\";\n");
                    }
                }
            } else {
                for (String string : methodDependencies.keySet()) {
                    for (String s : methodDependencies.get(string)) {
                        bufferedWriter.write("\t" + "\"" + string + "\" -> \"" + s + "\";\n");
                    }
                }
            }
            bufferedWriter.write("}");
            bufferedWriter.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //根据changeInfo.txt获取更改信息
    public static void getChangeInfo() {
        try {
            BufferedReader in = new BufferedReader(new FileReader(changeInfo));

            String line = in.readLine();
            while (!(line == null || "\n".equals(line))) {
                if (commandType == "-c") {
                    changeInfoList.add(line.toString().split(" ")[0]);
                } else {
                    changeInfoList.add(line);
                }
                line = in.readLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //选择测试方法
    public static void select() {

        for (String s : changeInfoList) {
            if (commandType == "-c") {
                //类模式下加入变更信息
                relatedClasses.add(s);
                //递归选择
                selectClassRecursive(relatedClasses, s);
            } else {
                //递归选择
                selectMethodRecursive(relatedMethods, s);
                selectedMethods.addAll(relatedMethods);
                for (String r : relatedMethods) {
                    if (testMethods.contains(r)) {
                        selectedMethods.add(r);
                    }
                }
            }
        }
        if (commandType == "-c") {
            for (String s : allClassesOfMethods) {
                if (relatedClasses.contains(s)) {
                    for (String method : allMethods) {
                        if (method.split(" ")[0].equals(s)) {
                            selectedMethods.add(method);
                        }
                    }
                }
            }
        }
    }

    //保存结果
    public static void saveTxt() {
        String filename = commandType == "-c" ? "./selection-class.txt" : "./selection-method.txt";
        try {
            BufferedWriter out = new BufferedWriter(new FileWriter(filename));
            for (String s : selectedMethods) {
                out.write(s + "\n");
            }
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //递归寻找所有依赖了searchClass的类加入到relatedClass中
    public static void selectClassRecursive(HashSet<String> relatedClasses, String searchClass) {
        for (String callee : callRelation.keySet()) {
            if (callee.split(" ")[0].equals(searchClass)) {
                for (String caller : callRelation.get(callee)) {
                    relatedClasses.add(caller.split(" ")[0]);
                    if (!relatedClasses.contains(caller.split(" ")[0])) {
                        selectClassRecursive(relatedClasses, caller.split(" ")[0]);
                    }
                }
            }
        }
    }

    //递归寻找所有依赖了searchMethod的类加入到relatedMethod中
    public static void selectMethodRecursive(HashSet<String> relatedMethod, String searchMethod) {
        HashSet<String> newMethod = new HashSet<String>();
        if (!callRelation.containsKey(searchMethod))
            return;
        for (String s : callRelation.get(searchMethod)) {
            if (!relatedMethod.contains(s)) {
                relatedMethod.add(s);
                newMethod.add(s);
            }
        }
        for (String s : newMethod) {
            selectMethodRecursive(relatedMethod, s);
        }
    }


}
