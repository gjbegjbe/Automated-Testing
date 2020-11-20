package org.example.testCaseSelector;

import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.shrikeCT.InvalidClassFileException;
import com.ibm.wala.util.CancelException;
import org.example.testCaseSelector.Analysis.Analysis;
import org.example.testCaseSelector.TestCaseSelector.SelectorByClass;
import org.example.testCaseSelector.TestCaseSelector.SelectorByMethod;
import org.example.testCaseSelector.TestCaseSelector.TestCaseSelector;
import org.example.testCaseSelector.enums.CommandType;

import java.io.IOException;

public class Main {
    private static TestCaseSelector testCaseSelector;

    public static void main(String[] args) throws CancelException, ClassHierarchyException, InvalidClassFileException, IOException {
        CommandType ct = selectCommand(args[0]);

        //分析部分
        Analysis ana = new Analysis(args);
        ana.analyze();

        switch (ct) {
            case CLASS:
                testCaseSelector = new SelectorByClass();
                testCaseSelector.select();
                break;
            case METHOD:
                testCaseSelector = new SelectorByMethod();
                testCaseSelector.select();
                break;
            case OTHER:
                System.out.println("Wrong!");
                return;
            default:
        }

    }

    public static CommandType selectCommand(String arg) {
        switch (arg) {
            case "-c":
                return CommandType.CLASS;
            case "-m":
                return CommandType.METHOD;
            default:
                return CommandType.OTHER;
        }
    }


}
