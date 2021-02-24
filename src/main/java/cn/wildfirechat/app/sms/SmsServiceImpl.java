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
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.services.sns.AmazonSNSClient;
import com.amazonaws.services.sns.AmazonSNSClientBuilder;
import com.amazonaws.services.sns.model.MessageAttributeValue;
import com.amazonaws.services.sns.model.PublishRequest;
import com.amazonaws.services.sns.model.PublishResult;
import com.github.qcloudsms.SmsSingleSender;
import com.github.qcloudsms.SmsSingleSenderResult;
import com.github.qcloudsms.httpclient.HTTPException;
import com.google.gson.Gson;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

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
            System.out.println("send sms code mobile:"+ mobile);
            String[] ms = mobile.split(" ");
            if (ms.length == 1) {
                return sendAliyunCode(mobile, code);
            } else if (ms[0].equals("+86")) {
                return sendAliyunCode(ms[1], code);
            } else {
               return sendAwsCode(mobile, code);
            }
        } else {
            return RestResult.RestCode.ERROR_SERVER_NOT_IMPLEMENT;
        }
    }

    private RestResult.RestCode sendTencentCode(String mobile, String code) {
        try {
            System.out.println("send  mobile:"+ mobile+"code:"+code);
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
        System.out.println("send  mobile:"+ mobile+"code:"+code);
        String content = String.format("【冉静网络】您的验证码是：%s。", code);

        FormBody formBody = new FormBody
                .Builder()
                .add("appid", "59717")//设置参数名称和参数值
                .add("content", content)
                .add("to", mobile)
                .add("signature", "e5c49e43cb8fc94e72ca53b3528499da")
                .build();
        try {
            String url = "https://api.mysubmail.com/message/send.json";
            String result = okHttpClient.newCall(
                    new Request.Builder().url(url).post(formBody).build()
            ).execute().body().string();
            System.out.println("send sms code result :" + result);
            return RestResult.RestCode.SUCCESS;
        } catch (Exception e) {
            return RestResult.RestCode.ERROR_SERVER_ERROR;
        }
    }

    private RestResult.RestCode sendAwsCode(String phoneNumber, String code) {
        System.out.println("send aws mobile:"+ phoneNumber);
        try {
            AWSCredentials awsCredentials = new AWSCredentials() {
                @Override
                public String getAWSAccessKeyId() {
                    return "AKIAUNZD3EQBV5H5UC64";
                }

                @Override
                public String getAWSSecretKey() {
                    return "dDUv67fj6KsJrjJkQwa+4mkABt0bXg2tFVbtBJHx";
                }
            };

            AmazonSNSClient snsClient = (AmazonSNSClient) AmazonSNSClientBuilder.standard().withRegion("ap-northeast-1").withCredentials(new AWSStaticCredentialsProvider(awsCredentials)).build();
            String message = "Your SMS verification code is: " + code;
            Map<String, MessageAttributeValue> smsAttributes =
                    new HashMap<>();
            //<set SMS attributes>
            sendSMSMessage(snsClient, message, phoneNumber, smsAttributes);
            return RestResult.RestCode.SUCCESS;
        } catch (Exception e) {
            return RestResult.RestCode.ERROR_SERVER_ERROR;
        }

    }


    public static void sendSMSMessage(AmazonSNSClient snsClient, String message,
                                      String phoneNumber, Map<String, MessageAttributeValue> smsAttributes) {
        PublishResult result = snsClient.publish(new PublishRequest()
                .withMessage(message)
                .withPhoneNumber(phoneNumber)
                .withMessageAttributes(smsAttributes));
        System.out.println("send: aws sms ：" + result); // Prints the message ID.
    }

}
