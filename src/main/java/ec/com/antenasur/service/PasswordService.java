package ec.com.antenasur.service;

import at.favre.lib.crypto.bcrypt.BCrypt;
import jakarta.ejb.Stateless;

@Stateless
public class PasswordService {

    private static final int BCRYPT_COST = 12;

    public String hashBcrypt(String password) {
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("La contrasena no puede estar vacia");
        }
        return BCrypt.withDefaults().hashToString(BCRYPT_COST, password.toCharArray());
    }

    public boolean verifyBcrypt(String password, String hash) {
        if (password == null || hash == null || hash.isEmpty()) {
            return false;
        }
        return BCrypt.verifyer().verify(password.toCharArray(), hash).verified;
    }
}
