package com.trae.loganalysis.service;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

public class FileAnalysisServiceTest {

    /**
     * 测试extractMethodCode方法是否能正确抽取sendCore方法的源码
     */
    @Test
    public void testExtractMethodCode() throws Exception {
        // 准备测试源码
        String fullSourceCode = "package cn.com.zybank.mcs.unsecured.loan.unsecured.loan.prcd.service.uslmang.service;\n\n" +
                "import cn.com.zybank.mcs.ncm.system.base.core.constant.SystemBaseConstant;\n\n" +
                "/**\n" +
                " * 与核心通信service\n" +
                " *\n" +
                " * @author yibo.su\n" +
                " * @date 2024/3/14 10:09\n" +
                " */\n\n" +
                "@Service\n" +
                "public class CoreConnectionService {\n\n" +
                "\tprivate static final Logger log = LoggerFactory.getLogger(ZyxjFileJobService.class);\n\n" +
                "\t/**\n" +
                "\t * 059149\n" +
                "\t * 客户下账户和客户级限制查询\n" +
                "\t * 客户号与证件信息二传一，优先推荐客户号\n" +
                "\t *\n" +
                "\t * @param customerNum 客户号\n" +
                "\t * @param certType    证件类型\n" +
                "\t * @param certNum     证件号\n" +
                "\t * @return left-请求是否成功，right-报文对象\n" +
                "\t */\n" +
                "\tpublic Pair<Boolean, JSONObject> customerAccountAndLimitQuery(String customerNum, String certType, String certNum) {\n" +
                "\t\t\tif (StringUtils.isBlank(customerNum) && paramFlag) {\n" +
                "\t\t\tlog.error(\"客户下账户和客户级限制查询-请求参数为空,customerNum:{},certType:{},certNum:{}\", customerNum, certType, certNum);\n" +
                "\t\t\treturn generateFailPair(\"必传参数为空\");\n" +
                "\t\t}\n\n" +
                "\t\t//组装报文头\n" +
                "\t\tMap<String, Object> reqMap = Maps.newHashMap();\n" +
                "\t\treqMap.putAll(ComXmlHeadUtil.sysCoreHeadMsg(tranCode, ConstantUtil.SYSTEM_CODE_CBS));\n" +
                "\t\t\n" +
                "\t\treqMap.put(\"SEQ_NO\", CurrentHeaderHolder.buildSeqNo());\n\n" +
                "\t\treturn sendCore(\"客户下账户和客户级限制查询\", tranCode, reqMap);\n" +
                "\t}\n\n" +
                "\t/**\n" +
                "\t * 发送请求至核心\n" +
                "\t *\n" +
                "\t * @param interfaceName 接口标识\n" +
                "\t * @param tranCode      请求参数\n" +
                "\t * @param reqMap        返回参数\n" +
                "\t * @return left-请求是否成功，right-报文对象\n" +
                "\t */\n" +
                "\tprivate Pair<Boolean, JSONObject> sendCore(String interfaceName, String tranCode, Map<String, Object> reqMap) {\n\n" +
                "\t\ttry {\n" +
                "\t\t\tlog.info(\"调用核心{}-{}，向核心发送报文:{}\", tranCode, interfaceName, JSONObject.toJSONString(reqMap));\n" +
                "\t\t} catch (Exception e) {\n" +
                "\t\t\treturn generateFailPair(e.getMessage());\n" +
                "\t\t}\n\n" +
                "\t\ttry {\n" +
                "\t\t\tJSONObject json = new JSONObject(respMap);\n" +
                "\t\t\tJSONObject result = json.getJSONObject(\"result\");\n" +
                "\t\t\tJSONObject replyMsgJson = result.getJSONObject(\"Reply_Msg\");\n" +
                "\t\t\tif (\"000000\".equals(retCode)) {\n" +
                "\t\t\t\treturn Pair.of(true, json);\n" +
                "\t\t\t} else {\n" +
                "\t\t\t\treturn Pair.of(false, json);\n" +
                "\t\t\t}\n" +
                "\t\t} catch (Exception e) {\n" +
                "\t\t\treturn generateFailPair(\"核心报文异常\");\n" +
                "\t\t}\n" +
                "\t}";

        // 准备测试参数
        String className = "CoreConnectionService";
        Integer lineNum = 150;
        String methodName = "sendCore";

        // 创建FileAnalysisService实例
        FileAnalysisService service = new FileAnalysisService(null, null, null, null, 1);

        // 使用反射调用私有方法
        Method extractMethodCodeMethod = FileAnalysisService.class.getDeclaredMethod("extractMethodCode", 
                String.class, String.class, Integer.class, String.class);
        extractMethodCodeMethod.setAccessible(true);

        // 调用方法
        String extractedCode = (String) extractMethodCodeMethod.invoke(service, 
                fullSourceCode, className, lineNum, methodName);

        // 验证结果
        assertNotNull(extractedCode, "抽取的方法源码不应为空");
        assertTrue(extractedCode.contains("sendCore"), 
                "抽取的源码应包含方法名");
        assertTrue(extractedCode.contains("interfaceName"), 
                "抽取的源码应包含interfaceName参数");
        assertTrue(extractedCode.contains("tranCode"), 
                "抽取的源码应包含tranCode参数");
        assertTrue(extractedCode.contains("reqMap"), 
                "抽取的源码应包含reqMap参数");
        assertTrue(extractedCode.contains("调用核心"), 
                "抽取的源码应包含日志内容");
        assertTrue(extractedCode.contains("return Pair.of"), 
                "抽取的源码应包含返回语句");
        assertTrue(extractedCode.contains("{") && extractedCode.contains("}"), 
                "抽取的源码应包含大括号");
        assertTrue(extractedCode.contains("generateFailPair"), 
                "抽取的源码应包含generateFailPair方法调用");

        // 打印抽取的源码
        System.out.println("=== 抽取的方法源码 ===");
        System.out.println(extractedCode);
        System.out.println("=== 抽取结束 ===");
    }

    /**
     * 测试extractMethodCode方法处理空输入的情况
     */
    @Test
    public void testExtractMethodCodeWithEmptyInput() throws Exception {
        // 创建FileAnalysisService实例
        FileAnalysisService service = new FileAnalysisService(null, null, null, null, 1);

        // 使用反射调用私有方法
        Method extractMethodCodeMethod = FileAnalysisService.class.getDeclaredMethod("extractMethodCode", 
                String.class, String.class, Integer.class, String.class);
        extractMethodCodeMethod.setAccessible(true);

        // 测试空源码
        String result1 = (String) extractMethodCodeMethod.invoke(service, "", "TestClass", 1, "testMethod");
        assertEquals("", result1, "空源码应返回空字符串");

        // 测试null源码
        String result2 = (String) extractMethodCodeMethod.invoke(service, null, "TestClass", 1, "testMethod");
        assertNull(result2, "null源码应返回null");

        // 测试空方法名
        String result3 = (String) extractMethodCodeMethod.invoke(service, "public class Test {}", "TestClass", 1, "");
        assertEquals("public class Test {}", result3, "空方法名应返回原始源码");
    }

    /**
     * 测试extractMethodCode方法处理不存在方法的情况
     */
    @Test
    public void testExtractMethodCodeWithNonExistentMethod() throws Exception {
        // 准备测试源码
        String fullSourceCode = "public class TestClass {\n" +
                "    public void existingMethod() {\n" +
                "        System.out.println(\"test\");\n" +
                "    }\n" +
                "}";

        // 创建FileAnalysisService实例
        FileAnalysisService service = new FileAnalysisService(null, null, null, null, 1);

        // 使用反射调用私有方法
        Method extractMethodCodeMethod = FileAnalysisService.class.getDeclaredMethod("extractMethodCode", 
                String.class, String.class, Integer.class, String.class);
        extractMethodCodeMethod.setAccessible(true);

        // 调用方法，传入不存在的方法名
        String extractedCode = (String) extractMethodCodeMethod.invoke(service, 
                fullSourceCode, "TestClass", 1, "nonExistentMethod");

        // 验证结果：未找到方法时应返回原始源码
        assertEquals(fullSourceCode, extractedCode, "未找到方法时应返回原始源码");
    }

    /**
     * 测试extractMethodCode方法处理嵌套大括号的情况
     */
    @Test
    public void testExtractMethodCodeWithNestedBraces() throws Exception {
        // 准备测试源码（包含嵌套大括号）
        String fullSourceCode = "public class TestClass {\n" +
                "    public void testMethod() {\n" +
                "        if (true) {\n" +
                "            if (true) {\n" +
                "                System.out.println(\"nested\");\n" +
                "            }\n" +
                "        }\n" +
                "    }\n" +
                "}";

        // 创建FileAnalysisService实例
        FileAnalysisService service = new FileAnalysisService(null, null, null, null, 1);

        // 使用反射调用私有方法
        Method extractMethodCodeMethod = FileAnalysisService.class.getDeclaredMethod("extractMethodCode", 
                String.class, String.class, Integer.class, String.class);
        extractMethodCodeMethod.setAccessible(true);

        // 调用方法
        String extractedCode = (String) extractMethodCodeMethod.invoke(service, 
                fullSourceCode, "TestClass", 1, "testMethod");

        // 验证结果
        assertTrue(extractedCode.contains("testMethod"), 
                "抽取的源码应包含方法名");
        assertTrue(extractedCode.contains("if (true)"), 
                "抽取的源码应包含if语句");
        assertTrue(extractedCode.contains("nested"), 
                "抽取的源码应包含嵌套内容");
        assertTrue(extractedCode.contains("{") && extractedCode.contains("}"), 
                "抽取的源码应包含大括号");

        // 打印抽取的源码
        System.out.println("=== 抽取的嵌套方法源码 ===");
        System.out.println(extractedCode);
        System.out.println("=== 抽取结束 ===");
    }
}