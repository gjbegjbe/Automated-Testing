package org.example.testCaseSelector.TestCaseSelector;

import org.example.testCaseSelector.Analysis.Analysis;
import org.example.testCaseSelector.DotGraph.DotByClass;
import org.example.testCaseSelector.DotGraph.DotGenerator;
import org.example.testCaseSelector.FileWriter.FileWriter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class SelectorByClass implements TestCaseSelector {
    private DotGenerator dotGenerator = new DotByClass();
    private HashSet<String> selectedTests = new HashSet<>();
    private HashSet<String> changes = Analysis.getChanges();
    // 测试类和方法
    private HashMap<String, HashSet<String>> testMethodsOfTestClasses = Analysis.getTestMethodsOfTestClasses();
    // 调用图顶点
    private ArrayList<String> vertexList;
    // 调用图邻接表
    private ArrayList<ArrayList<Integer>> adjacentList;

    public void select() {
        before();
        // 寻找更改的类
        HashSet<String> changedClasses = new HashSet<>();
        for (String change : changes) {
            changedClasses.add(change.split(" ")[0]);
        }

        // 选择测试类
        for (String changedClass : changedClasses) {
            dfs(new HashSet<>(), vertexList.indexOf(changedClass));
        }

        writeFile();
    }

    private void before() {
        dotGenerator.generateDot();
        vertexList = dotGenerator.getVertexList();
        adjacentList = dotGenerator.getAdjacentList();
    }

    private void writeFile() {
        String content = "";
        for (String test : selectedTests) {
            content += test + '\n';
        }
        FileWriter.writeFile("selection-class.txt", content);
    }


    //深度优先搜索
    private void dfs(HashSet<Integer> visited, int curIndex) {
        if (visited.contains(curIndex))
            return;
        visited.add(curIndex);
        for (int callerIndex : adjacentList.get(curIndex)) {
            String caller = vertexList.get(callerIndex);
            if (testMethodsOfTestClasses.containsKey(caller)) {
                HashSet<String> methods = testMethodsOfTestClasses.get(caller);
                for (String method : methods) {
                    selectedTests.add(caller + ' ' + method);
                }
            }
            dfs(visited, callerIndex);
        }
    }
}
