package com.example;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.StdTypeResolverBuilder;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import org.junit.Test;

import java.util.Optional;

public class OptionalJacksonTest {

    @Test
    public void test() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setDefaultTyping(
                new StdTypeResolverBuilder()
                        .init(JsonTypeInfo.Id.CLASS, null)
                        .inclusion(JsonTypeInfo.As.WRAPPER_OBJECT)
        );
        mapper.registerModule(new Jdk8Module());
        String json = mapper.writeValueAsString(new Foo(Optional.of(1)));
        System.out.println(json);
        mapper.readValue(json, Foo.class);
    }

    public static class Foo {
        private Optional<Integer> bar;

        public Foo() {
        }

        public Foo(Optional<Integer> bar) {
            this.bar = bar;
        }

        public Optional<Integer> getBar() {
            return bar;
        }

        public void setBar(Optional<Integer> bar) {
            this.bar = bar;
        }
    }
}