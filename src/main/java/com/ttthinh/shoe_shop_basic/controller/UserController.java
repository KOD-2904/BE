package com.ttthinh.shoe_shop_basic.controller;

import com.ttthinh.shoe_shop_basic.dto.request.RegisterRequest;
import com.ttthinh.shoe_shop_basic.dto.response.ApiResponse;
import com.ttthinh.shoe_shop_basic.dto.response.UserResponse;
import com.ttthinh.shoe_shop_basic.service.UserService;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("")
public class UserController {
    @Autowired
    private UserService userService;

    @PostMapping("/register")
    public ApiResponse<UserResponse> register(
            @Valid @RequestBody RegisterRequest registerRequest
    ) {
        UserResponse userResponse = userService.register(registerRequest);
        return ApiResponse.<UserResponse>builder()
                .code(200)
                .message("Registered Successfully")
                .result(userResponse)
                .build();
        //return ApiResponse.success("Registered Successfully",userResponse);
    }

    @GetMapping("/users")
    public ApiResponse<List<UserResponse>> getUsers() {
        var list = userService.getAllUsers();
        return ApiResponse.<List<UserResponse>>builder()
                .code(200)
                .message("Found " + list.size() + " users")
                .result(list)
                .build();
    }
    @GetMapping("/myinfor")
    public ApiResponse<UserResponse> getMyInformation() {
        var userResponse = userService.getMyInformation();
        return ApiResponse.<UserResponse>builder()
                .message("Get Information Successfully")
                .result(userResponse)
                .build();
    }

    @GetMapping("/inforbyid/{id}")
    public ApiResponse<UserResponse> getMyInformationById(@PathVariable String id) {
        return ApiResponse.<UserResponse>builder()
                .code(200)
                .message("Get Information Successfully")
                .result(userService.getMyInformationById(id))
                .build();
    }
    @DeleteMapping("/users/delete")
    public ApiResponse deleteAllUser() {
        userService.deleteAllUsers();
        return ApiResponse.builder()
                .code(200)
                .message("Deleted Successfully")
                .build();
    }
}
