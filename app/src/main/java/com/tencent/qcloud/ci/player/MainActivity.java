/*
 * Copyright (c) 2010-2020 Tencent Cloud. All rights reserved.
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in all
 *  copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 */

package com.tencent.qcloud.ci.player;


import static com.tencent.qcloud.ci.player.VideoPlayerActivity.EXTRA_TAG;
import static com.tencent.qcloud.ci.player.VideoPlayerActivity.EXTRA_URL;

import android.content.Intent;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

import com.tencent.qcloud.ci.player.assistor.CIMediaInfo;
import com.tencent.qcloud.ci.player.assistor.CIPlayerAssistor;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    // 原始的媒体url，请替换成您业务的url，此处url仅为示例
    private final String orgUrl = "https://ci-h5-bj-1258125638.cos.ap-beijing.myqcloud.com/hls/BigBuckBunny.m3u8?ci-process=pm3u8";
    // 用于在子线程请求网络 获取token和授权
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private EditText etUrl;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewById(R.id.btn_m3u8).setOnClickListener(this);
        findViewById(R.id.btn_m3u8_encryption).setOnClickListener(this);
        findViewById(R.id.btn_m3u8_encryption1).setOnClickListener(this);
        etUrl = findViewById(R.id.etUrl);
        etUrl.setText(orgUrl);

        // 初始化万象播放协助器
        CIPlayerAssistor.getInstance().init(this);
        CIPlayerAssistor.getInstance().setLogEnable(BuildConfig.DEBUG);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_m3u8:
                // CIMediaInfo实例，可用于请求token
                CIMediaInfo standardCiMediaInfo = new CIMediaInfo(etUrl.getText().toString(), false);
                executorService.submit(() -> {
                    // 从业务服务器获取token和授权信息: 自行实现getTokenAndAuthoriz方法
                    Pair<String, String> pair = getTokenAndAuthorization(standardCiMediaInfo.getMediaUrl(), standardCiMediaInfo.getPublicKey());
                    // 给ciMediaInfo设置获取到的token和授权信息
                    standardCiMediaInfo.setToken(pair.first);
                    /*
                     * 设置授权信息，会在url上通过&拼接传入的authorization
                     * 如果原始url是cdn的话，不用传cos的authorization
                     */
                    standardCiMediaInfo.setAuthorization(pair.second);
                    // 获取最终的播放url
                    String url = CIPlayerAssistor.getInstance().buildPlayerUrl(standardCiMediaInfo);
                    String tag = "标准加密M3U8";
                    runOnUiThread(() -> startActivity(url, tag));
                });
                break;
            case R.id.btn_m3u8_encryption:
                // CIMediaInfo实例，可用于请求token
                CIMediaInfo ciMediaInfo = new CIMediaInfo(etUrl.getText().toString());
                executorService.submit(() -> {
                    // 从业务服务器获取token和授权信息: 自行实现getTokenAndAuthoriz方法
                    Pair<String, String> pair = getTokenAndAuthorization(ciMediaInfo.getMediaUrl(), ciMediaInfo.getPublicKey());
                    // 给ciMediaInfo设置获取到的token和授权信息
                    ciMediaInfo.setToken(pair.first);
                    /*
                     * 设置授权信息，会在url上通过&拼接传入的authorization
                     * 如果原始url是cdn的话，不用传cos的authorization
                     */
                    ciMediaInfo.setAuthorization(pair.second);
                    // 获取最终的播放url
                    String url = CIPlayerAssistor.getInstance().buildPlayerUrl(ciMediaInfo);
                    String tag = "私有加密M3U8";
                    runOnUiThread(() -> startActivity(url, tag));
                });
                break;
            case R.id.btn_m3u8_encryption1:
                // 生成rsa密钥对
                KeyPairGenerator keyPairGenerator = null;
                try {
                    keyPairGenerator = KeyPairGenerator.getInstance("RSA");
                } catch (NoSuchAlgorithmException e) {
                    throw new RuntimeException(e);
                }
                keyPairGenerator.initialize(1024, new SecureRandom()); // 1024 is the keysize.
                KeyPair keyPair = keyPairGenerator.generateKeyPair();
                PublicKey publicKey = keyPair.getPublic();
                PrivateKey privateKey = keyPair.getPrivate();
                // 注意此处的base64 flags为Base64.DEFAULT
                String publicKeyString = Base64.encodeToString(publicKey.getEncoded(), Base64.DEFAULT);
                String privateKeyString = Base64.encodeToString(privateKey.getEncoded(), Base64.DEFAULT);
                // 格式化公钥
                String headlinePublic = "-----BEGIN PUBLIC KEY-----\n";
                String footlinePublic = "-----END PUBLIC KEY-----";
                String rsaPublicKey = headlinePublic + publicKeyString + footlinePublic;
//                String rsaPublicKey = "-----BEGIN PUBLIC KEY-----\n" +
//                        "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCzn58LpxDyvMHOaaX1d32Dp2nX\n" +
//                        "IleS6+cziqP0EQIC3XUsGYArFXY+25Q5oEf+p0jdT3XvngjFr3cWPaQnNCBjDoav\n" +
//                        "nmXLiAbgSpdPmgnKEREiK1t7sJd3DiSDwVHIas0XMLsoxmIibYgFju7IG3P+H9LG\n" +
//                        "aq4yBb/ldMGUjRyM5QIDAQAB\n" +
//                        "-----END PUBLIC KEY-----";
                Log.i("CIPlayerAssistor", rsaPublicKey);

                // 格式化私钥
                String headlinePrivate = "-----BEGIN RSA PRIVATE KEY-----\n";
                String footlinePrivate = "-----END RSA PRIVATE KEY-----";
                String rsaPrivateKey = headlinePrivate + privateKeyString + footlinePrivate;
//                String rsaPrivateKey = "-----BEGIN RSA PRIVATE KEY-----\n" +
//                        "MIICXQIBAAKBgQCzn58LpxDyvMHOaaX1d32Dp2nXIleS6+cziqP0EQIC3XUsGYAr\n" +
//                        "FXY+25Q5oEf+p0jdT3XvngjFr3cWPaQnNCBjDoavnmXLiAbgSpdPmgnKEREiK1t7\n" +
//                        "sJd3DiSDwVHIas0XMLsoxmIibYgFju7IG3P+H9LGaq4yBb/ldMGUjRyM5QIDAQAB\n" +
//                        "AoGASofL3XDnxmBt5jDODMkUymDXuM1mGu9JUoiPOQEpnXi4WqEGHlpcYv6HRVXt\n" +
//                        "KYvN3w5OeCtRpn0E47SV/TJS0TUxsloUAv4+jtahh5JXQgbkaMPKwEZeljHh1krB\n" +
//                        "8U1ls9Ze37CSccjTiPI2j5QL199v34pMJ078SE/UWdR+7I0CQQDd6n4/NktaEuma\n" +
//                        "rDbVM88rzVV1mlC8GXoPr4Qdd7UMUpWl1uBi/CR4/15Iwh0nJLka3PdI3j4D1kiE\n" +
//                        "mKLeJkC3AkEAzzY7aNJ/TTPxEKyEzEPn8+srFdce/Q0KeucicreNoyetg/ma9x/j\n" +
//                        "/2xKZWfum78VKheWYbpl+9W30vEEgPxLQwJBANJANVSWkFXKzWEqANmGuKX7aRh/\n" +
//                        "GDbevHLYDAgPPo1qQTZam0WtNrEc4at1nkFT3bzB1dhIF+FyyEo4gzaOb4kCQGz0\n" +
//                        "OzDW3hEeKrd1vy4Y1rIvCaymPKkaXlRZjId9dIBBsL8gVBd1MVFxA31mtNQ4GUzU\n" +
//                        "skIY3N8adVn5WUDpaDkCQQC3lrukk/TOoSmlUlrCPX5d9HLtAI4SNUGcB1ya+BAE\n" +
//                        "zcAOo6Fzpox8WT3h3aBz1CIrLYMgqS3dNDIhS6A+VC04\n" +
//                        "-----END RSA PRIVATE KEY-----";
                Log.i("CIPlayerAssistor", rsaPrivateKey);

                // CIMediaInfo实例，自定义私钥
                CIMediaInfo privateKeyCiMediaInfo = new CIMediaInfo(etUrl.getText().toString(), rsaPrivateKey);
                executorService.submit(() -> {
                    // 获取token和授权信息: 自行实现getTokenAndAuthoriz方法
                    Pair<String, String> pair = getTokenAndAuthorization(privateKeyCiMediaInfo.getMediaUrl(), rsaPublicKey);
                    // 给privateKeyCiMediaInfo设置获取到的token和授权信息
                    privateKeyCiMediaInfo.setToken(pair.first);
                    /*
                     * 设置授权信息，会在url上通过&拼接传入的authorization
                     * 如果原始url是cdn的话，不用传cos的authorization
                     */
                    privateKeyCiMediaInfo.setAuthorization(pair.second);
                    // 获取最终的播放url
                    String url = CIPlayerAssistor.getInstance().buildPlayerUrl(privateKeyCiMediaInfo);
                    String tag = "私有加密M3U8";
                    runOnUiThread(() -> startActivity(url, tag));
                });
                break;
        }
    }

    private void startActivity(String url, String tag){
        Intent intent = new Intent(this, VideoPlayerActivity.class);
        intent.putExtra(EXTRA_URL, url);
        intent.putExtra(EXTRA_TAG, tag);
        startActivity(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 用完播放器记得销毁
        CIPlayerAssistor.getInstance().destroy();
    }

    /**
     * 从业务服务器获取token和授权
     */
    private Pair<String, String> getTokenAndAuthorization(String mediaUrl, String publicKey) {
        HttpURLConnection urlConnection = null;
        try {
            // 该url仅为示例，请替换成您业务的url，具体实现请参考 “业务后端示例代码”
            URL url = new URL("https://cos.cloud.tencent.com/samples/hls/token");
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("POST");
            urlConnection.setDoOutput(true);
            urlConnection.setRequestProperty("Content-Type", "application/json");

            JSONObject jsonObject = new JSONObject();
            // 添加键值对到JSONObject
            jsonObject.put("src", mediaUrl);
            jsonObject.put("protectContentKey", publicKey != null?1:0);
            if(publicKey != null) {
                byte[] publicKeyData = publicKey.getBytes(StandardCharsets.UTF_8);
                jsonObject.put("publicKey", Base64.encodeToString(publicKeyData, Base64.DEFAULT));
            }
            // 将JSONObject转换为字符串
            String jsonInputString = jsonObject.toString();

            byte[] input = jsonInputString.getBytes("utf-8");
            urlConnection.getOutputStream().write(input, 0, input.length);

            int status = urlConnection.getResponseCode();
            if (status != 200) {
                throw new RuntimeException("HttpResponseCode: " + status);
            } else {
                BufferedReader br = new BufferedReader(new InputStreamReader(urlConnection.getInputStream(), "utf-8"));
                StringBuilder response = new StringBuilder();
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
                String responseString = response.toString();
                Log.d("getToKen：", responseString);
                JSONObject json = new JSONObject(responseString);
                String token = json.getString("token");
                String authorization = json.getString("authorization");
                // 在这里你可以使用token和authorization
                return new Pair<>(token, authorization);
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
    }
}