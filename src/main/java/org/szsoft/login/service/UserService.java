package org.szsoft.login.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import dev.samstevens.totp.secret.SecretGenerator;
import dev.samstevens.totp.util.Utils;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.code.HashingAlgorithm;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.qr.QrGenerator;
import dev.samstevens.totp.qr.ZxingPngQrGenerator;
import dev.samstevens.totp.exceptions.QrGenerationException;
import org.szsoft.login.model.Role;
import org.szsoft.login.model.User;
import org.szsoft.login.repository.RoleRepository;
import org.szsoft.login.repository.UserRepository;

import java.util.Arrays;
import java.util.HashSet;

@Service
public class UserService {

    private UserRepository userRepository;
    private RoleRepository roleRepository;
    private BCryptPasswordEncoder bCryptPasswordEncoder;

    @Autowired
    public UserService(UserRepository userRepository,
                       RoleRepository roleRepository,
                       BCryptPasswordEncoder bCryptPasswordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.bCryptPasswordEncoder = bCryptPasswordEncoder;
    }

    public User findUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public User findUserByUserName(String userName) {
        return userRepository.findByUserName(userName);
    }

    public User saveUser(User user) throws QrGenerationException {
        user.setPassword(bCryptPasswordEncoder.encode(user.getPassword()));
        user.setRut(user.getRut().replace(".", ""));
        user.setActive(true);
        user.setEmail(user.getUserName());
        Role userRole = roleRepository.findByRole("ADMIN");
        user.setRoles(new HashSet<Role>(Arrays.asList(userRole)));
        String secret = getSecret();
        String qrcode = getQRCode(user.getEmail(), secret);
        user.setSecret(secret);
        user.setQrCode(qrcode);
        return userRepository.save(user);
    }

    private String getSecret() {
    	SecretGenerator secretGenerator = new DefaultSecretGenerator();
    	return secretGenerator.generate();    	    	
    }
    
    private String getQRCode(String email, String secret) throws QrGenerationException {
    	QrGenerator qrGenerator = new ZxingPngQrGenerator();
        QrData data = new QrData.Builder().label(email)
                .secret(secret)
                .issuer("Gobierno de Chile")
                .algorithm(HashingAlgorithm.SHA256)
                .digits(6)
                .period(30)
                .build();
        return  Utils.getDataUriForImage(
                qrGenerator.generate(data),
                qrGenerator.getImageMimeType()
        );
    }
    
}