package com.my.ftpdemo;

import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Environment;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import org.apache.ftpserver.ConnectionConfigFactory;
import org.apache.ftpserver.FtpServer;
import org.apache.ftpserver.FtpServerFactory;
import org.apache.ftpserver.ftplet.Authentication;
import org.apache.ftpserver.ftplet.AuthenticationFailedException;
import org.apache.ftpserver.ftplet.Authority;
import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.ftplet.User;
import org.apache.ftpserver.ftplet.UserManager;
import org.apache.ftpserver.listener.ListenerFactory;
import org.apache.ftpserver.usermanager.AnonymousAuthentication;
import org.apache.ftpserver.usermanager.ClearTextPasswordEncryptor;
import org.apache.ftpserver.usermanager.PasswordEncryptor;
import org.apache.ftpserver.usermanager.PropertiesUserManagerFactory;
import org.apache.ftpserver.usermanager.UserManagerFactory;
import org.apache.ftpserver.usermanager.UsernamePasswordAuthentication;
import org.apache.ftpserver.usermanager.impl.AbstractUserManager;
import org.apache.ftpserver.usermanager.impl.BaseUser;
import org.apache.ftpserver.usermanager.impl.ConcurrentLoginPermission;
import org.apache.ftpserver.usermanager.impl.WritePermission;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by Administrator on 2018.03.28.
 */

public class FtpService extends Service {


    private FtpServer server;
    private String user = "admin";
    private String password = "123456";
    private static String rootPath;
    private int port = 2221;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        rootPath = "/sdcard/";//Environment.getExternalStorageDirectory().getAbsolutePath();

        try {
            init();
            Toast.makeText(this, "启动ftp服务成功", Toast.LENGTH_SHORT).show();
        } catch (FtpException e) {
            e.printStackTrace();
            Toast.makeText(this, "启动ftp服务失败", Toast.LENGTH_SHORT).show();
        }

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        release();
        Toast.makeText(this, "关闭ftp服务", Toast.LENGTH_SHORT).show();
    }

    /**
     * 初始化
     *
     * @throws FtpException
     */
    public void init() throws FtpException {
        release();
        startFtp();
    }

    private void startFtp() throws FtpException {
        FtpServerFactory serverFactory = new FtpServerFactory();

        //设置访问用户名和密码还有共享路径
//        BaseUser baseUser = new BaseUser();
//        baseUser.setName("anonymou");
//        //baseUser.setPassword(password);
//        baseUser.setHomeDirectory(rootPath);
//
//        List<Authority> authorities = new ArrayList<Authority>();
//        authorities.add(new WritePermission());
//        baseUser.setAuthorities(authorities);
//        serverFactory.getUserManager().save(baseUser);

        //设置无需账户密码访问
        ConnectionConfigFactory connectionConfigFactory = new ConnectionConfigFactory();
        connectionConfigFactory.setAnonymousLoginEnabled(true);
        serverFactory.setConnectionConfig(connectionConfigFactory.createConnectionConfig());
        serverFactory.setUserManager(new TestUserManagerFactory().createUserManager());

        ListenerFactory factory = new ListenerFactory();
        //PropertiesUserManagerFactory userManagerFactory = new PropertiesUserManagerFactory();
        //serverFactory.setUserManager(userManagerFactory.createUserManager());
        factory.setPort(port); //设置端口号 非ROOT不可使用1024以下的端口
        serverFactory.addListener("default", factory.createListener());

        server = serverFactory.createServer();
        server.start();
    }



    /**
     * 释放资源
     */
    public void release() {
        stopFtp();
    }

    private void stopFtp() {
        if (server != null) {
            server.stop();
            server = null;
        }
    }

    private class TestUserManagerFactory implements UserManagerFactory {

        @Override
        public UserManager createUserManager() {
            return new TestUserManager("admin", new ClearTextPasswordEncryptor());
        }
    }
    public String TEST_USERNAME = user;
    public String TEST_PASSWORD = password;
    private class TestUserManager extends AbstractUserManager {
        private BaseUser testUser;
        private BaseUser anonUser;

        public TestUserManager(String adminName, PasswordEncryptor passwordEncryptor) {
            super(adminName, passwordEncryptor);

            testUser = new BaseUser();
            testUser.setAuthorities(Arrays.asList(new Authority[] {new ConcurrentLoginPermission(1, 1)}));
            testUser.setEnabled(true);
            testUser.setHomeDirectory(rootPath);
            testUser.setMaxIdleTime(10000);
            testUser.setName(TEST_USERNAME);
            testUser.setPassword(TEST_PASSWORD);

            anonUser = new BaseUser(testUser);
            anonUser.setName("anonymous");
        }

        @Override
        public User getUserByName(String username) throws FtpException {
            if(TEST_USERNAME.equals(username)) {
                return testUser;
            } else if(anonUser.getName().equals(username)) {
                return anonUser;
            }

            return null;
        }

        @Override
        public String[] getAllUserNames() throws FtpException {
            return new String[] {TEST_USERNAME, anonUser.getName()};
        }

        @Override
        public void delete(String username) throws FtpException {
            //no opt
        }

        @Override
        public void save(User user) throws FtpException {
            //no opt
            System.out.println("save");
        }

        @Override
        public boolean doesExist(String username) throws FtpException {
            return (TEST_USERNAME.equals(username) || anonUser.getName().equals(username)) ? true : false;
        }

        @Override
        public User authenticate(Authentication authentication) throws AuthenticationFailedException {
            if(UsernamePasswordAuthentication.class.isAssignableFrom(authentication.getClass())) {
                UsernamePasswordAuthentication upAuth = (UsernamePasswordAuthentication) authentication;

                if(TEST_USERNAME.equals(upAuth.getUsername()) && TEST_PASSWORD.equals(upAuth.getPassword())) {
                    return testUser;
                }

                if(anonUser.getName().equals(upAuth.getUsername())) {
                    return anonUser;
                }
            } else if(AnonymousAuthentication.class.isAssignableFrom(authentication.getClass())) {
                return anonUser;
            }

            return null;
        }
    }

}
