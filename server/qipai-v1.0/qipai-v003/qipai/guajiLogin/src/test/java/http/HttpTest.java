package http;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.sy.sanguo.common.util.CoderUtil;
import com.sy.sanguo.common.util.webxinlang.HttpUtil;
import com.sy.sanguo.game.utils.AESUtil;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class HttpTest {
    private static final String http_data_aes_key = "dKPVJ60PJS8mlONb";
    private static final String http_data_aes_id = "KbzG2kNBuQe8RrMW";


    public static void main(String[] args) throws Exception {
        test415();
    }

    public static void test415() throws Exception {
        String url = "http://127.0.0.1:8381/pdklogin/d415.action";
        Map<String, String> params = new HashMap<>();
        String userId = "120008";
        String sessCode = "da0e315f33358a1186df651849f0d0cb";
        String pageNo = "1";
        String pageSize = "10";
        params.put("userId", userId);
        params.put("sessCode", sessCode);
        params.put("pageNo", pageNo);
        params.put("pageSize", pageSize);
        params.put("sign", genSign(params));
        String aesParamStr = aesEncrypt(JSON.toJSONString(params));
        String paramStr = CoderUtil.encode(aesParamStr + "" + http_data_aes_id);
        String ret = HttpUtil.post(url + "?" + paramStr, null);
        String decryptRet = aesDecrypt(ret);
        JSONObject jsonObject = JSON.parseObject(decryptRet);
        System.out.println(jsonObject.toString());
    }

    public static String genSign(Map<String, String> params) {
        params.put("timestamp", System.currentTimeMillis() / 1000 + "");
        String[] objs = params.keySet().toArray(new String[0]);
        Arrays.sort(objs);
        StringBuilder stringBuilder = new StringBuilder();
        for (String obj : objs) {
            stringBuilder.append("&").append(obj).append("=").append(params.get(obj));
        }
        String signKey = "043a528a8fc7195876fc4d4c3eaa7d2e";
        stringBuilder.append("&key=").append(signKey);
        return com.sy.sanguo.common.util.request.MD5Util.getMD5String(stringBuilder);
    }

    public static String aesEncrypt(String data) {
        return AESUtil.encrypt(data, http_data_aes_key);
    }

    public static String aesDecrypt(String data) {
        return AESUtil.decrypt(data, http_data_aes_key);
    }

}
