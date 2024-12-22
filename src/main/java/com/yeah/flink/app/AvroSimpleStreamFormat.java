package com.yeah.flink.app;

import org.apache.avro.Schema;
import org.apache.avro.file.DataFileReader;
import org.apache.avro.file.SeekableInput;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.flink.annotation.PublicEvolving;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.connector.file.src.reader.SimpleStreamFormat;
import org.apache.flink.connector.file.src.reader.StreamFormat;
import org.apache.flink.core.fs.FSDataInputStream;
import org.apache.flink.formats.avro.utils.FSDataInputStreamWrapper;

import javax.annotation.Nullable;
import java.io.IOException;

public class AvroSimpleStreamFormat <T> extends SimpleStreamFormat <T> {
    public Schema schema;

    public AvroSimpleStreamFormat(Schema schema) {
        this.schema = schema;
    }

    @Override
    public Reader<T> createReader(Configuration config, FSDataInputStream stream) throws IOException{
        SeekableInput seekableInput = new FSDataInputStreamWrapper(stream, 0);
        SpecificDatumReader<T> specificDatumReader = new SpecificDatumReader<>(schema);
        DataFileReader<T> dataFileReader = new DataFileReader<>(seekableInput, specificDatumReader);
        return new Reader<>(dataFileReader);
    }

    @Override
    public TypeInformation<T> getProducedType() {
        return (TypeInformation<T>) TypeInformation.of(GenericRecord.class);
    }

    @PublicEvolving
    public static final class Reader<T> implements StreamFormat.Reader<T> {
        private final DataFileReader<T> reader;

        Reader(final DataFileReader<T> reader) {
            this.reader = reader;
        }

        @Nullable
        @Override
        public T read() {
            return reader.hasNext()? reader.next(): null;
        }

        @Override
        public void close() throws IOException {
            reader.close();
        }
    }
}
