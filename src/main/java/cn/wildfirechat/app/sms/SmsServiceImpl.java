package cn.wildfirechat.app.sms;

import cn.wildfirechat.app.RestResult;
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

    static OkHttpClient okHttpClient = new OkHttpClient();

    private RestResult.RestCode sendAliyunCode(String mobile, String code) {
        try {
            String url = String.format("http://47.101.58.161:18002/send.do?uid=26455&pw=175194&mb=%s&ms=【YL】您的验证码：%s，10分钟内有效，请勿向任何人泄漏。&ex=77", mobile, code);
            String result = okHttpClient.newCall(
                    new Request.Builder().url(url).get().build()
            ).execute().body().string();
            System.out.println("send sms code result :" + result);
            return RestResult.RestCode.SUCCESS;
        } catch (Exception e) {
            return RestResult.RestCode.ERROR_SERVER_ERROR;
        }
    }

}
