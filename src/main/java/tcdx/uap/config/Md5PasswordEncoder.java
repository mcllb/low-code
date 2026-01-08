package tcdx.uap.config;

import org.springframework.security.crypto.password.PasswordEncoder;
import tcdx.uap.common.utils.Md5Utils;

/**
 * Md5加密
 */
public class Md5PasswordEncoder implements PasswordEncoder {
    @Override
    public String encode(CharSequence rawPassword) {
        return Md5Utils.hash(rawPassword.toString());
    }

    @Override
    public boolean matches(CharSequence rawPassword, String encodedPassword) {
        return Md5Utils.hash(rawPassword.toString()).equals(encodedPassword);
    }
}
