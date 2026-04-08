package com.ttthinh.shoe_shop_basic.service.impl.authImpl;

import com.ttthinh.shoe_shop_basic.dto.request.auth.RegisterRequest;
import com.ttthinh.shoe_shop_basic.dto.response.auth.UserResponse;
import com.ttthinh.shoe_shop_basic.entity.auth.Role;
import com.ttthinh.shoe_shop_basic.entity.auth.UserAccount;
import com.ttthinh.shoe_shop_basic.entity.verify.EmailVerifyToken;
import com.ttthinh.shoe_shop_basic.enums.UserStatus;
import com.ttthinh.shoe_shop_basic.exception.AppException;
import com.ttthinh.shoe_shop_basic.exception.ErrorCode;
import com.ttthinh.shoe_shop_basic.mapper.UserMapper;
import com.ttthinh.shoe_shop_basic.repository.auth.EmailVerifyRepository;
import com.ttthinh.shoe_shop_basic.repository.auth.RoleRepository;
import com.ttthinh.shoe_shop_basic.repository.auth.UserAccountRepository;
import com.ttthinh.shoe_shop_basic.service.auth.MailService;
import com.ttthinh.shoe_shop_basic.service.auth.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
//import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private static final String baseUrl = "http://localhost:8080";

    private final UserAccountRepository userAccountRepository;

    private final RoleRepository roleRepository;

    private final PasswordEncoder passwordEncoder;

    private final UserMapper userMapper;
    private final EmailVerifyRepository emailVerifyRepository;
    private final MailService mailService;
//    @Autowired
//    private PathPatternRequestMatcher.Builder builder;


    @Override
    public UserResponse register(RegisterRequest request) {

        if(userAccountRepository.existsByUsername(request.getUsername())) {
            throw new AppException(ErrorCode.USERNAME_EXIST);
        }
        // 1. Check trùng email/phone
        if (userAccountRepository.existsByEmail(request.getEmail())) {
            throw new AppException(ErrorCode.EMAIL_EXIST);
        }
        if (request.getPhone() != null && userAccountRepository.existsByPhone(request.getPhone())) {
            throw new AppException(ErrorCode.PHONE_EXIST);
        }


        // 2. Lấy role mặc định ROLE_USER
        HashSet<Role> roles = new HashSet<>();

        Role roleUser = roleRepository.findByCode("ROLE_USER")
                .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_EXIST));
        roles.add(roleUser);

        UserAccount userAccount = userMapper.toUser(request);
        userAccount.setRoles(roles);
        userAccount.setPassword(passwordEncoder.encode(request.getPassword()));
        userAccount.setEmailVerified(false);
        userAccount.setStatus(UserStatus.INACTIVE);

        userAccount = userAccountRepository.save(userAccount);

        String rawToken = UUID.randomUUID().toString();
        EmailVerifyToken emailVerifyToken = EmailVerifyToken.builder()
                .token(rawToken)
                .user(userAccount)
                .expiresAt(Instant.now().plus(Duration.ofMinutes(30))) // 30 phút
                .used(false)
                .build();

        emailVerifyRepository.save(emailVerifyToken);
        String link = baseUrl + "/auth/verify-email?token=" + rawToken;
        mailService.sendMail(request.getEmail(), link);



        return userMapper.toUserResponse(userAccount);
    }

    @Override
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public List<UserResponse> getAllUsers() {
//        log.info("getAllUsers");
//        var authentication = SecurityContextHolder.getContext().getAuthentication();
//        assert authentication != null;
//        authentication.getAuthorities().forEach(grantedAuthority -> log.info(grantedAuthority.getAuthority()));
        var list = userAccountRepository.findAll();
        return list.stream().map(userMapper::toUserResponse).collect(Collectors.toList());
    }

    @Override
    @PostAuthorize("returnObject.username == authentication.name")
    public UserResponse getMyInformation() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        assert authentication != null;
        var username = authentication.getName();
        var user = userAccountRepository.findByUsername(username).orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXIST));
        return userMapper.toUserResponse(user);
    }
    @Override
    @PostAuthorize("returnObject.id == authentication.name")
    public UserResponse getMyInformationById(String id) {
        UserAccount user = userAccountRepository.findById(id).orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXIST));
        return userMapper.toUserResponse(user);
    }
    @Override
    public void deleteAllUsers() {
        userAccountRepository.deleteAll();
    }
}
