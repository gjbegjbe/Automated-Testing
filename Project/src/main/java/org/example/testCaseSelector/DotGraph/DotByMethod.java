package org.example.testCaseSelector.DotGraph;

import com.ibm.wala.classLoader.ShrikeBTMethod;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import org.example.testCaseSelector.Analysis.Analysis;
import org.example.testCaseSelector.FileWriter.FileWriter;

import java.util.ArrayList;
import java.util.Iterator;

public class DotByMethod implements DotGenerator {
    private CallGraph callGraph = Analysis.getCallGraph();
    // 顶点
    private ArrayList<String> vertexList = new ArrayList<>();
    // 邻接表
    private ArrayList<ArrayList<Integer>> adjacentList = new ArrayList<>();

    @Override
    public ArrayList<String> getVertexList() {
        return vertexList;
    }

    @Override
    public ArrayList<ArrayList<Integer>> getAdjacentList() {
        return adjacentList;
    }


    @Override
    public void generateDot() {
        extractGraph();

        String projectName = Analysis.getProjectName();
        String dotContent = toDot(projectName, vertexList, adjacentList);
        FileWriter.writeFile("method-" + projectName + ".dot", dotContent);
    }

    private void extractGraph() {
        for (CGNode node : callGraph) {
            if (node.getMethod() instanceof ShrikeBTMethod) {
                ShrikeBTMethod method = (ShrikeBTMethod) node.getMethod();
                if ("Application".equals(method.getDeclaringClass().getClassLoader().toString())) {
                    String classInnerName = method.getDeclaringClass().getName().toString();
                    String signature = method.getSignature();
                    String vertex = classInnerName + ' ' + signature;
                    if (!vertexList.contains(vertex)) {
                        vertexList.add(vertex);
                        adjacentList.add(new ArrayList<>());
                    }
                }
            }
        }
        // 获取邻接表
        for (CGNode node : callGraph) {
            if (node.getMethod() instanceof ShrikeBTMethod) {
                ShrikeBTMethod method = (ShrikeBTMethod) node.getMethod();
                if ("Application".equals(method.getDeclaringClass().getClassLoader().toString())) {
                    String classInnerName = method.getDeclaringClass().getName().toString();
                    String signature = method.getSignature();
                    String callee = classInnerName + ' ' + signature;
                    int calleeIndex = vertexList.indexOf(callee);
                    Iterator<CGNode> callerNodesItr = callGraph.getPredNodes(node);
                    while (callerNodesItr.hasNext()) {
                        CGNode callerNode = callerNodesItr.next();
                        if (callerNode.getMethod() instanceof ShrikeBTMethod) {
                            ShrikeBTMethod callerMethod = (ShrikeBTMethod) callerNode.getMethod();
                            if ("Application".equals(callerMethod.getDeclaringClass().getClassLoader().toString())) {
                                String callerInnerName = callerMethod.getDeclaringClass().getName().toString();
                                String callerSigniture = callerMethod.getSignature();
                                String caller = callerInnerName + ' ' + callerSigniture;
                                Integer callerIndex = vertexList.indexOf(caller);
                                if (!adjacentList.get(calleeIndex).contains(callerIndex))
                                    adjacentList.get(calleeIndex).add(callerIndex);
                            }
                        }
                    }

                }
            }
        }
    }

    private String toDot(String projectName, ArrayList<String> vertexList, ArrayList<ArrayList<Integer>> adjacentList) {
        String dotContent = "digraph " + projectName + " {\n";
        for (int calleeIndex = 0; calleeIndex < vertexList.size(); calleeIndex++) {
            String callee = vertexList.get(calleeIndex);
            if (adjacentList.get(calleeIndex).isEmpty())
                continue;
            for (int callerIndex : adjacentList.get(calleeIndex)) {
                String caller = vertexList.get(callerIndex);
                dotContent += "\t\"" + callee + "\" -> \"" + caller + "\";\n";
            }
        }
        dotContent.trim();
        dotContent += "}";
        return dotContent;
    }


}
