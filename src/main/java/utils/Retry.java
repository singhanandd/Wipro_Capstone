package utils;

import org.testng.IRetryAnalyzer;
import org.testng.ITestResult;

public class Retry implements IRetryAnalyzer {
    private int count = 0;
    private final int max = 1; // retry once
    @Override
    public boolean retry(ITestResult result) { return count++ < max; }
}
