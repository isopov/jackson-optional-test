package com.example;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import com.fasterxml.jackson.databind.deser.Deserializers;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonFormatVisitorWrapper;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.jsontype.impl.StdTypeResolverBuilder;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;
import com.fasterxml.jackson.databind.ser.Serializers;
import com.fasterxml.jackson.databind.ser.impl.PropertySerializerMap;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.fasterxml.jackson.databind.type.ReferenceType;
import com.fasterxml.jackson.databind.util.NameTransformer;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import org.junit.Test;

import java.io.IOException;
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
        mapper.registerModule(new OldOptionalSerializersModule());
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

    private static class OldOptionalSerializersModule extends Module {

        @Override
        public String getModuleName() {
            return "OldOptionalSerializerModule";
        }

        @Override
        public Version version() {
            return Version.unknownVersion();
        }

        @Override
        public void setupModule(SetupContext context) {
            context.addSerializers(new OldOptionalSerializers());
            context.addDeserializers(new OldOptionalDeserializers());
        }
    }

    //Below is raw copy&paste from Jackson 2.8.11 (anly classes renamed)
    private static class OldOptionalSerializers extends Serializers.Base {
        @Override
        public JsonSerializer<?> findReferenceSerializer(SerializationConfig config,
                                                         ReferenceType refType, BeanDescription beanDesc,
                                                         TypeSerializer contentTypeSerializer, JsonSerializer<Object> contentValueSerializer) {
            final Class<?> raw = refType.getRawClass();
            if (Optional.class.isAssignableFrom(raw)) {
                return new OldOptionalSerializer(refType, contentTypeSerializer, contentValueSerializer);
            }
            return null;
        }
    }

    private static class OldOptionalDeserializers extends Deserializers.Base {
        // 21-Oct-2015, tatu: Code much simplified with 2.7 where we should be getting much
        //    of boilerplate handling automatically

        @Override // since 2.7
        public JsonDeserializer<?> findReferenceDeserializer(ReferenceType refType,
                                                             DeserializationConfig config, BeanDescription beanDesc,
                                                             TypeDeserializer contentTypeDeserializer, JsonDeserializer<?> contentDeserializer) {
            if (refType.hasRawClass(Optional.class)) {
                return new OldOptionalDeserializer(refType, contentTypeDeserializer, contentDeserializer);
            }
            return null;
        }
    }


    private static class OldOptionalSerializer
            extends StdSerializer<Optional<?>>
            implements ContextualSerializer {
        private static final long serialVersionUID = 1L;

        /**
         * Declared type parameter for Optional.
         */
        protected final JavaType _referredType;

        protected final BeanProperty _property;

        protected final TypeSerializer _valueTypeSerializer;
        protected final JsonSerializer<Object> _valueSerializer;

        /**
         * To support unwrapped values of dynamic types, will need this:
         */
        protected final NameTransformer _unwrapper;

        /**
         * Further guidance on serialization-inclusion (or not), regarding
         * contained value (if any).
         *
         * @since 2.7
         */
        protected final JsonInclude.Include _contentInclusion;

        /**
         * If element type can not be statically determined, mapping from
         * runtime type to serializer is handled using this object
         *
         * @since 2.6
         */
        protected transient PropertySerializerMap _dynamicSerializers;

    /*
    /**********************************************************
    /* Constructors, factory methods
    /**********************************************************
     */

        @Deprecated // since 2.7
        public OldOptionalSerializer(JavaType type) {
            this((ReferenceType) type, null, null);
        }

        @SuppressWarnings("unchecked")
        protected OldOptionalSerializer(ReferenceType optionalType,
                                        TypeSerializer vts, JsonSerializer<?> valueSer) {
            super(optionalType);
            _referredType = optionalType.getReferencedType();
            _property = null;
            _valueTypeSerializer = vts;
            _valueSerializer = (JsonSerializer<Object>) valueSer;
            _unwrapper = null;
            _contentInclusion = null;
            _dynamicSerializers = PropertySerializerMap.emptyForProperties();
        }

        @SuppressWarnings("unchecked")
        protected OldOptionalSerializer(OldOptionalSerializer base, BeanProperty property,
                                        TypeSerializer vts, JsonSerializer<?> valueSer,
                                        NameTransformer unwrapper, JsonInclude.Include contentIncl) {
            super(base);
            _referredType = base._referredType;
            _dynamicSerializers = base._dynamicSerializers;
            _property = property;
            _valueTypeSerializer = vts;
            _valueSerializer = (JsonSerializer<Object>) valueSer;
            _unwrapper = unwrapper;
            if ((contentIncl == JsonInclude.Include.USE_DEFAULTS)
                    || (contentIncl == JsonInclude.Include.ALWAYS)) {
                _contentInclusion = null;
            } else {
                _contentInclusion = contentIncl;
            }
        }

        @Override
        public JsonSerializer<Optional<?>> unwrappingSerializer(NameTransformer transformer) {
            JsonSerializer<Object> ser = _valueSerializer;
            if (ser != null) {
                ser = ser.unwrappingSerializer(transformer);
            }
            NameTransformer unwrapper = (_unwrapper == null) ? transformer
                    : NameTransformer.chainedTransformer(transformer, _unwrapper);
            return withResolved(_property, _valueTypeSerializer, ser, unwrapper, _contentInclusion);
        }

        protected OldOptionalSerializer withResolved(BeanProperty prop,
                                                     TypeSerializer vts, JsonSerializer<?> valueSer,
                                                     NameTransformer unwrapper, JsonInclude.Include contentIncl) {
            if ((_property == prop) && (contentIncl == _contentInclusion)
                    && (_valueTypeSerializer == vts) && (_valueSerializer == valueSer)
                    && (_unwrapper == unwrapper)) {
                return this;
            }
            return new OldOptionalSerializer(this, prop, vts, valueSer, unwrapper, contentIncl);
        }

    /*
    /**********************************************************
    /* Contextualization (support for property annotations)
    /**********************************************************
     */

        @Override
        public JsonSerializer<?> createContextual(SerializerProvider provider,
                                                  BeanProperty property) throws JsonMappingException {
            TypeSerializer vts = _valueTypeSerializer;
            if (vts != null) {
                vts = vts.forProperty(property);
            }
            JsonSerializer<?> ser = findContentSerializer(provider, property);
            if (ser == null) {
                ser = _valueSerializer;
                if (ser == null) {
                    // A few conditions needed to be able to fetch serializer here:
                    if (_useStatic(provider, property, _referredType)) {
                        ser = _findSerializer(provider, _referredType, property);
                    }
                } else {
                    ser = provider.handlePrimaryContextualization(ser, property);
                }
            }
            // Also: may want to have more refined exclusion based on referenced value
            JsonInclude.Include contentIncl = _contentInclusion;
            if (property != null) {
                JsonInclude.Value incl = property.findPropertyInclusion(provider.getConfig(),
                        Optional.class);
                JsonInclude.Include newIncl = incl.getContentInclusion();
                if ((newIncl != contentIncl) && (newIncl != JsonInclude.Include.USE_DEFAULTS)) {
                    contentIncl = newIncl;
                }
            }
            return withResolved(property, vts, ser, _unwrapper, contentIncl);
        }

        protected boolean _useStatic(SerializerProvider provider, BeanProperty property,
                                     JavaType referredType) {
            // First: no serializer for `Object.class`, must be dynamic
            if (referredType.isJavaLangObject()) {
                return false;
            }
            // but if type is final, might as well fetch
            if (referredType.isFinal()) { // or should we allow annotation override? (only if requested...)
                return true;
            }
            // also: if indicated by typing, should be considered static
            if (referredType.useStaticType()) {
                return true;
            }
            // if neither, maybe explicit annotation?
            AnnotationIntrospector intr = provider.getAnnotationIntrospector();
            if ((intr != null) && (property != null)) {
                Annotated ann = property.getMember();
                if (ann != null) {
                    JsonSerialize.Typing t = intr.findSerializationTyping(property.getMember());
                    if (t == JsonSerialize.Typing.STATIC) {
                        return true;
                    }
                    if (t == JsonSerialize.Typing.DYNAMIC) {
                        return false;
                    }
                }
            }
            // and finally, may be forced by global static typing (unlikely...)
            return provider.isEnabled(MapperFeature.USE_STATIC_TYPING);
        }

    /*
    /**********************************************************
    /* API overrides
    /**********************************************************
     */

        @Override
        public boolean isEmpty(SerializerProvider provider, Optional<?> value) {
            if ((value == null) || !value.isPresent()) {
                return true;
            }
            if (_contentInclusion == null) {
                return false;
            }
            Object contents = value.get();
            JsonSerializer<Object> ser = _valueSerializer;
            if (ser == null) {
                try {
                    ser = _findCachedSerializer(provider, contents.getClass());
                } catch (JsonMappingException e) { // nasty but necessary
                    throw new RuntimeJsonMappingException(e);
                }
            }
            return ser.isEmpty(provider, contents);
        }

        @Override
        public boolean isUnwrappingSerializer() {
            return (_unwrapper != null);
        }

    /*
    /**********************************************************
    /* Serialization methods
    /**********************************************************
     */

        @Override
        public void serialize(Optional<?> opt, JsonGenerator gen, SerializerProvider provider)
                throws IOException {
            if (!opt.isPresent()) {
                // [datatype-jdk8#20]: can not write `null` if unwrapped
                if (_unwrapper == null) {
                    provider.defaultSerializeNull(gen);
                }
                return;
            }
            Object value = opt.get();
            JsonSerializer<Object> ser = _valueSerializer;
            if (ser == null) {
                ser = _findCachedSerializer(provider, value.getClass());
            }
            if (_valueTypeSerializer != null) {
                ser.serializeWithType(value, gen, provider, _valueTypeSerializer);
            } else {
                ser.serialize(value, gen, provider);
            }
        }

        @Override
        public void serializeWithType(Optional<?> opt,
                                      JsonGenerator gen, SerializerProvider provider,
                                      TypeSerializer typeSer) throws IOException {
            if (!opt.isPresent()) {
                // [datatype-jdk8#20]: can not write `null` if unwrapped
                if (_unwrapper == null) {
                    provider.defaultSerializeNull(gen);
                }
                return;
            }
            // Otherwise apply type-prefix/suffix, then std serialize:
            typeSer.writeTypePrefixForScalar(opt, gen, Optional.class);
            serialize(opt, gen, provider);
            typeSer.writeTypeSuffixForScalar(opt, gen);
        }

    /*
    /**********************************************************
    /* Introspection support
    /**********************************************************
     */

        @Override
        public void acceptJsonFormatVisitor(JsonFormatVisitorWrapper visitor, JavaType typeHint) throws JsonMappingException {
            JsonSerializer<?> ser = _valueSerializer;
            if (ser == null) {
                ser = _findSerializer(visitor.getProvider(), _referredType, _property);
                if (_unwrapper != null) {
                    ser = ser.unwrappingSerializer(_unwrapper);
                }
            }
            ser.acceptJsonFormatVisitor(visitor, _referredType);
        }

    /*
    /**********************************************************
    /* Misc other
    /**********************************************************
     */

        /**
         * Helper method that encapsulates logic of retrieving and caching required
         * serializer.
         */
        private final JsonSerializer<Object> _findCachedSerializer(SerializerProvider provider,
                                                                   Class<?> type) throws JsonMappingException {
            JsonSerializer<Object> ser = _dynamicSerializers.serializerFor(type);
            if (ser == null) {
                ser = _findSerializer(provider, type, _property);
                if (_unwrapper != null) {
                    ser = ser.unwrappingSerializer(_unwrapper);
                }
                _dynamicSerializers = _dynamicSerializers.newWith(type, ser);
            }
            return ser;
        }

        private final JsonSerializer<Object> _findSerializer(SerializerProvider provider,
                                                             Class<?> type, BeanProperty prop) throws JsonMappingException {
            // 13-Mar-2017, tatu: To fix [modules-java8#17] change `findTypedValueSerializer()` int `findValueSerializer()`
            return provider.findValueSerializer(type, prop);
        }

        private final JsonSerializer<Object> _findSerializer(SerializerProvider provider,
                                                             JavaType type, BeanProperty prop) throws JsonMappingException {
            // 13-Mar-2017, tatu: To fix [modules-java8#17] change `findTypedValueSerializer()` int `findValueSerializer()`
            return provider.findValueSerializer(type, prop);
        }

        // !!! 22-Mar-2016, tatu: Method added in jackson-databind 2.7.4; may be used
        //    when we go 2.8
        protected JsonSerializer<?> findContentSerializer(SerializerProvider serializers,
                                                          BeanProperty property)
                throws JsonMappingException {
            if (property != null) {
                // First: if we have a property, may have property-annotation overrides
                AnnotatedMember m = property.getMember();
                if (m != null) {
                    final AnnotationIntrospector intr = serializers.getAnnotationIntrospector();
                    Object serDef = intr.findContentSerializer(m);
                    if (serDef != null) {
                        return serializers.serializerInstance(m, serDef);
                    }
                }
            }
            return null;
        }
    }

    private static class OldOptionalDeserializer
            extends StdDeserializer<Optional<?>>
            implements ContextualDeserializer {
        private static final long serialVersionUID = 1L;

        protected final JavaType _fullType;

        protected final JsonDeserializer<?> _valueDeserializer;
        protected final TypeDeserializer _valueTypeDeserializer;

    /*
    /**********************************************************
    /* Life-cycle
    /**********************************************************
     */

        public OldOptionalDeserializer(JavaType fullType,
                                       TypeDeserializer typeDeser, JsonDeserializer<?> valueDeser) {
            super(fullType);
            _fullType = fullType;
            _valueTypeDeserializer = typeDeser;
            _valueDeserializer = valueDeser;
        }

        /**
         * Overridable fluent factory method used for creating contextual
         * instances.
         */
        protected OldOptionalDeserializer withResolved(TypeDeserializer typeDeser,
                                                       JsonDeserializer<?> valueDeser) {
            if ((valueDeser == _valueDeserializer) && (typeDeser == _valueTypeDeserializer)) {
                return this;
            }
            return new OldOptionalDeserializer(_fullType, typeDeser, valueDeser);
        }

        /**
         * Method called to finalize setup of this deserializer,
         * after deserializer itself has been registered. This
         * is needed to handle recursive and transitive dependencies.
         */
        @Override
        public JsonDeserializer<?> createContextual(DeserializationContext ctxt,
                                                    BeanProperty property) throws JsonMappingException {
            JsonDeserializer<?> deser = _valueDeserializer;
            TypeDeserializer typeDeser = _valueTypeDeserializer;
            JavaType refType = _fullType.getReferencedType();

            if (deser == null) {
                deser = ctxt.findContextualValueDeserializer(refType, property);
            } else { // otherwise directly assigned, probably not contextual yet:
                deser = ctxt.handleSecondaryContextualization(deser, property, refType);
            }
            if (typeDeser != null) {
                typeDeser = typeDeser.forProperty(property);
            }
            return withResolved(typeDeser, deser);
        }

    /*
    /**********************************************************
    /* Overridden accessors
    /**********************************************************
     */

        @Override
        public JavaType getValueType() {
            return _fullType;
        }

        @Override
        public Optional<?> getNullValue(DeserializationContext ctxt) {
            return Optional.empty();
        }

    /*
    /**********************************************************
    /* Deserialization
    /**********************************************************
     */

        @Override
        public Optional<?> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            Object refd = (_valueTypeDeserializer == null)
                    ? _valueDeserializer.deserialize(p, ctxt)
                    : _valueDeserializer.deserializeWithType(p, ctxt, _valueTypeDeserializer);
            return Optional.ofNullable(refd);
        }

        @Override
        public Optional<?> deserializeWithType(JsonParser p, DeserializationContext ctxt, TypeDeserializer typeDeserializer)
                throws IOException {
            final JsonToken t = p.getCurrentToken();
            if (t == JsonToken.VALUE_NULL) {
                return getNullValue(ctxt);
            }
            // 03-Nov-2013, tatu: This gets rather tricky with "natural" types
            //   (String, Integer, Boolean), which do NOT include type information.
            //   These might actually be handled ok except that nominal type here
            //   is `Optional`, so special handling is not invoked; instead, need
            //   to do a work-around here.
            // 22-Oct-2015, tatu: Most likely this is actually wrong, result of incorrewct
            //   serialization (up to 2.6, was omitting necessary type info after all);
            //   but safest to leave in place for now
            if (t != null && t.isScalarValue()) {
                return deserialize(p, ctxt);
            }
            return (Optional<?>) typeDeserializer.deserializeTypedFromAny(p, ctxt);
        }
    }
}