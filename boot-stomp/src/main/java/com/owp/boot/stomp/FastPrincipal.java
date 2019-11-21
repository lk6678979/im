package com.owp.boot.stomp;

import java.security.Principal;

public class FastPrincipal implements Principal {

    private final String name;

    public FastPrincipal(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}