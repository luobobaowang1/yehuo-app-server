package cn.wildfirechat.app.sms;

import cn.wildfirechat.app.RestResult;
import cn.wildfirechat.app.sms.request.SmsSendRequest;
import cn.wildfirechat.app.sms.response.SmsSendResponse;
import com.alibaba.fastjson.JSON;
import com.aliyuncs.CommonRequest;
import com.aliyuncs.CommonResponse;
import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.exceptions.ClientException;
import com.aliyuncs.exceptions.ServerException;
import com.aliyuncs.http.MethodType;
import com.aliyuncs.profile.DefaultProfile;
import com.github.qcloudsms.SmsSingleSender;
import com.github.qcloudsms.SmsSingleSenderResult;
import com.github.qcloudsms.httpclient.HTTPException;
import com.google.gson.Gson;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

@Service
public class SmsServiceImpl implements SmsService {
    private static final Logger LOG = LoggerFactory.getLogger(SmsServiceImpl.class);


    private static class AliyunCommonResponse {
        String Message;
        String Code;
    }

    @Value("${sms.verdor}")
    private int smsVerdor;

    @Autowired
    private TencentSMSConfig mTencentSMSConfig;

    @Autowired
    private AliyunSMSConfig aliyunSMSConfig;

    @Override
    public RestResult.RestCode sendCode(String mobile, String code) {
        if (smsVerdor == 1) {
            return sendTencentCode(mobile, code);
        } else if (smsVerdor == 2) {
            return sendAliyunCode(mobile, code);
        } else {
            return RestResult.RestCode.ERROR_SERVER_NOT_IMPLEMENT;
        }
    }

    private RestResult.RestCode sendTencentCode(String mobile, String code) {
        try {
            String[] params = {code};
            SmsSingleSender ssender = new SmsSingleSender(mTencentSMSConfig.appid, mTencentSMSConfig.appkey);
            SmsSingleSenderResult result = ssender.sendWithParam("86", mobile,
                    mTencentSMSConfig.templateId, params, null, "", "");
            if (result.result == 0) {
                return RestResult.RestCode.SUCCESS;
            } else {
                LOG.error("Failure to send SMS {}", result);
                return RestResult.RestCode.ERROR_SERVER_ERROR;
            }
        } catch (HTTPException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return RestResult.RestCode.ERROR_SERVER_ERROR;
    }

    private RestResult.RestCode sendAliyunCode(String phone, String code) {
        try {
            //请求地址请登录253云通讯自助通平台查看或者询问您的商务负责人获取
            String smsSingleRequestServerUrl = "https://sms.253.com/msg/send/json";
            //短信内容
            String msg = "【来思齐】您的验证为 " + code;
            //状态报告
            String report = "true";

            SmsSendRequest smsSingleRequest = new SmsSendRequest(account, password, msg, phone, report);

            String requestJson = JSON.toJSONString(smsSingleRequest);

            System.out.println("before request string is: " + requestJson);

            String response = ChuangLanSmsUtil.sendSmsByPost(smsSingleRequestServerUrl, requestJson);

            System.out.println("response after request result is :" + response);

            SmsSendResponse smsSingleResponse = JSON.parseObject(response, SmsSendResponse.class);

            System.out.println("response  toString is :" + smsSingleResponse);
            return RestResult.RestCode.SUCCESS;
        } catch (Exception e) {
            return RestResult.RestCode.ERROR_SERVER_ERROR;
        }
    }


    // 用户平台API账号(非登录账号,示例:N1234567)
    public static String account = "YZM3653665";
    // 用户平台API密码(非登录密码)
    public static String password = "qdcQJmeVvKb9de";

}
