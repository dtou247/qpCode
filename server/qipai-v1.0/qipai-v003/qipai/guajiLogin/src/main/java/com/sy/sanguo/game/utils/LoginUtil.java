package com.sy.sanguo.game.utils;

import com.alibaba.fastjson.TypeReference;
import com.sy.sanguo.common.util.*;
import com.sy.sanguo.game.bean.RegInfo;
import com.sy.sanguo.game.bean.Server;
import com.sy.sanguo.game.bean.User;
import com.sy.sanguo.game.dao.UserDaoImpl;
import com.sy.sanguo.game.pdkuai.user.Manager;
import com.sy599.game.util.MD5Util;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

import java.util.*;

public class LoginUtil {

    public static final String pf_phoneNum = "phoneLogin";

    public static final String pf_self = "self";

    private static String md5_key_phoneNum = "sanguo_shangyou_2013";

    private static String aes_key_phoneNum = "je9AfMt2BEapgxqz";


    public static void setMd5KeyPhoneNum(String md5Key) {
        if (StringUtils.isNotBlank(md5Key)) {
            md5_key_phoneNum = md5Key;
        }
    }

    public static void setAESKeyPhoneNum(String aesKey) {
        if (StringUtils.isNotBlank(aesKey) && aesKey.length() == 16) {
            aes_key_phoneNum = aesKey;
        }
    }

    /**
     * 加密手机号
     *
     * @param phoneNum
     * @return
     */
    public static String encryptPhoneNumMD5(String phoneNum) {
        return MD5Util.getStringMD5(phoneNum + md5_key_phoneNum, "utf-8");
    }

    /**
     * AES加密手机号
     *
     * @param phoneNum
     * @return
     */
    public static String encryptPhoneNumAES(String phoneNum) {
        return AESUtil.encrypt(phoneNum, aes_key_phoneNum);
    }


    /**
     * AES 解密手机号
     *
     * @param phoneNum
     * @return
     */
    public static String decryptPhoneNumAES(String phoneNum) {
        return AESUtil.decrypt(phoneNum, aes_key_phoneNum);
    }

    /**
     * AES 解密手机号
     *
     * @param phoneNum
     * @return
     */
    public static String decryptPhoneNumAES(String phoneNum, String key) {
        return AESUtil.decrypt(phoneNum, key);
    }


    /**
     * 加密密码
     *
     * @param source
     * @return
     */
    public static String genPw(String source) {
        return com.sy.sanguo.common.util.MD5Util.getStringMD5(source + "sanguo_shangyou_2013");
    }

    /**
     * 生成session code
     *
     * @param username
     * @return
     */
    public static String genSessCode(String username) {
        StringBuilder sb = new StringBuilder();
        sb.append(username);
        sb.append(MathUtil.mt_rand(10000, 99999));
        sb.append(System.currentTimeMillis());
        return com.sy.sanguo.common.util.MD5Util.getStringMD5(sb.toString());
    }

    /**
     * 构建User对象
     *
     * @param userInfo
     * @return
     */
    public static User buildUser(RegInfo userInfo, List<Server> serverList) {
        User user = new User();
        user.setUsername(userInfo.getFlatId());
        user.setPf(userInfo.getPf());
        if (!StringUtils.isBlank(userInfo.getPlayedSid())) {
            if (serverList != null&&serverList.size()>0) {
                // 服务器列表
                List<Integer> servers = JacksonUtil.readValue(userInfo.getPlayedSid(), new TypeReference<List<Integer>>() {
                });
                // 服务器ID列表
                List<Integer> serverIdList = new ArrayList<Integer>();
                for (Server server : serverList) {
                    serverIdList.add(server.getId());
                }
                // 不在服务器列表内
                Iterator<Integer> iterator = servers.iterator();
                while (iterator.hasNext()) {
                    int serverId = iterator.next();
                    if (!serverIdList.contains(serverId)) {
                        iterator.remove();
                    }
                }
                user.setPlayedSid(JacksonUtil.writeValueAsString(servers));
            } else {
                user.setPlayedSid(userInfo.getPlayedSid());
            }

        } else {
            user.setPlayedSid("[]");
        }
        // servers = JacksonUtil.readValue(userInfo.getPlayedSid(), new
        // TypeReference<List<Integer>>() {
        // });
        user.setSessCode(userInfo.getSessCode());
        return user;
    }

    public static Map<String, Object> selfRegister(Map<String, String> params, UserDaoImpl userDao) throws Exception {
        return selfRegister(params, userDao, true);
    }

    public static Map<String, Object> selfRegister(Map<String, String> params, UserDaoImpl userDao, boolean checkSign) throws Exception {
        Map<String, Object> result = new HashMap<>();
        String username = params.get("u");
        String nickName = params.get("nickName");
        String password = params.get("ps");
        String platform = pf_self;
        String channel = params.get("c");// 渠道
        String deviceCode = params.get("deviceCode");// 设备码
        String headimgurl = params.get("headimgurl");// 头像

        if (checkSign) {
            String sign = params.get("k");
            long time = NumberUtils.toLong(params.get("t"));
            String secret = "mwFLeKLzNoL46dDn0vE2";
            StringBuilder md5 = new StringBuilder();
            md5.append(username == null ? "" : username);
            md5.append(password == null ? "" : password);
            md5.append(time);
            md5.append(platform);
            md5.append(channel);
            md5.append(secret);
            if (!com.sy.sanguo.common.util.MD5Util.getStringMD5(md5.toString()).equals(sign)) {
                result.put("code", 4);
                result.put("msg", "md5验证失败");
                return result;
            }
        }

        // 验证用户名和密码合法性
        if (StringUtils.isBlank(username) || !StringUtil.checkUserNameForSelfRegister(username)) {
            result.put("code", 992);
            result.put("msg", "账号不合法，字母开头，6到20位");
            return result;
        }
        if (StringUtils.isBlank(nickName)) {
            result.put("code", 991);
            result.put("msg", "请输入昵称");
            return result;
        }
        if (nickName.equals(username)) {
            result.put("code", 991);
            result.put("msg", "昵称不能与账号相同");
            return result;
        }
        int len = StringUtil.lengthOfNickName(nickName);
        if (len == 0 || len > 12) {
            result.put("code", 994);
            result.put("msg", "昵称过长");
            return result;
        }
        String checkRes = StringUtil.checkPassword(password);
        if (null != checkRes) {
            result.put("code", 993);
            result.put("msg", checkRes);
            return result;
        }

        String filt = KeyWordsFilter.getInstance_1().filt(nickName);
        if (!nickName.equals(filt)) {
            result.put("code", 1);
            result.put("msg", "昵称不合法：\n【" + nickName + "】-->【" + filt + "】");
            return result;
        }

        RegInfo regInfo;
        regInfo = userDao.getUser(username, platform);
        if (regInfo != null) {
            // 用户已存在
            result.put("code", 1);
            result.put("msg", "用户名已存在");
            return result;
        }

        // 注册成功
        long maxId = Manager.getInstance().generatePlayerId();
        regInfo = new RegInfo();
        regInfo.setFlatId(username);
        int sex = new Random().nextInt(100) >= 70 ? Constants.SEX_FEMALE : Constants.SEX_MALE;
        regInfo.setSex(sex);
        regInfo.setName(nickName);
        regInfo.setPw(genPw(password));
        regInfo.setSessCode(genSessCode(username));
        if (StringUtils.isNotBlank(deviceCode)) {
            regInfo.setDeviceCode(deviceCode);
        }
        if (StringUtils.isBlank(headimgurl)) {
            headimgurl = randomHeadimgurl();
        }
        regInfo.setHeadimgurl(headimgurl);
        Manager.getInstance().buildBaseUser(regInfo, platform, maxId);
        long ret = userDao.addUser(regInfo);
        if (ret <= 0) {
            result.put("code", 1);
            result.put("msg", "用户名已存在");
            return result;
        }
        regInfo.setUserId(ret);
        regInfo.setPf(platform);
        result.put("code", 0);
        result.put("user", buildUser(regInfo, Collections.<Server>emptyList()));
        return result;
    }

    public static String randomHeadimgurl() {
        return "res/res_icon/" + (1 + new Random().nextInt(30)) + ".png";
    }

    public static void main(String[] args) {
        String phoneNum = "15575222762";
        System.out.println(encryptPhoneNumMD5(phoneNum));


        String encrypt = encryptPhoneNumAES(phoneNum);
        System.out.println("AES|encrypt|" + phoneNum + "|" + encrypt + "|");

        encrypt = "j74yW+xes783u03vNSGCJQ==";
        String decrypt = decryptPhoneNumAES(encrypt);
        System.out.println("AES|decrypt|" + encrypt + "|" + decrypt + "|");

    }


}
