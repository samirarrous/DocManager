package com.example.demo.user

import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/users")
class UserController(
    private val userService: UserService
) {

    @GetMapping
    fun getAll(): List<User> {
        return userService.findAll()
    }

    @PostMapping
    fun create(@RequestBody user: User): User {
        return userService.createUser(user)
    }
}
