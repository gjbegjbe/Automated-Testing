package org.example.testCaseSelector.TestCaseSelector;

import org.example.testCaseSelector.Analysis.Analysis;
import org.example.testCaseSelector.DotGraph.DotByMethod;
import org.example.testCaseSelector.DotGraph.DotGenerator;
import org.example.testCaseSelector.FileWriter.FileWriter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class SelectorByMethod implements TestCaseSelector {
    private DotGenerator dotGenerator = new DotByMethod();
    private HashSet<String> selectedTests = new HashSet<>();
    private HashSet<String> changes = Analysis.getChanges();
    // 测试类和测试方法
    private HashMap<String,HashSet<String>> testMethodsOfTestClasses = Analysis.getTestMethodsOfTestClasses();
    // 调用图顶点
    private ArrayList<String> vertexList;
    // 调用图邻接表
    private ArrayList<ArrayList<Integer>> adjacentList;


    @Override
    public void select() {
        before();

        for (String change : changes) {
            dfs(new HashSet<>(), vertexList.indexOf(change));
        }
        writeFile();
    }



    private void before(){
        dotGenerator.generateDot();
        vertexList = dotGenerator.getVertexList();
        adjacentList = dotGenerator.getAdjacentList();
    }

    private void writeFile(){
        String content = "";
        for (String test : selectedTests) {
            content += test + '\n';
        }
        FileWriter.writeFile("selection-method.txt", content);
    }

    private void dfs(HashSet<Integer> visited, int curIndex) {
        if (visited.contains(curIndex))
            return;
        visited.add(curIndex);
        for (int callerIndex : adjacentList.get(curIndex)) {
            String caller = vertexList.get(callerIndex);
            String[] callerSplited = caller.split(" ");
            String classInnerName = callerSplited[0];
            String signature = callerSplited[1];
            if (testMethodsOfTestClasses.containsKey(classInnerName)){
                if (testMethodsOfTestClasses.get(classInnerName).contains(signature))
                selectedTests.add(caller);
            }
            dfs(visited, callerIndex);
        }
    }
}
