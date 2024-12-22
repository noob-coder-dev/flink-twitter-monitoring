package com.yeah.flink.app;

import org.apache.avro.Schema;
import org.apache.avro.file.DataFileReader;
import org.apache.avro.file.SeekableInput;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.connector.file.src.util.CheckpointedPosition;
import org.apache.flink.connector.file.src.reader.StreamFormat;
import org.apache.flink.core.fs.FSDataInputStream;

import java.io.IOException;

public class AvroStreamFormat<T> implements StreamFormat<T> {
    private final Schema schema;
    private final Class<T> type;

    public AvroStreamFormat(Schema schema, Class<T> type) {
        this.schema = schema;
        this.type = type;
    }

    @Override
    public Reader<T> createReader(Configuration config, FSDataInputStream stream, long splitOffset, long splitEnd) throws IOException {
        SeekableInput seekableInput = new FSDataSeekableInput(stream, splitOffset, splitEnd);
        DataFileReader<T> avroReader;

        if (GenericRecord.class.isAssignableFrom(type)) {
            avroReader = new DataFileReader<>(seekableInput, new GenericDatumReader<>(schema));
        } else {
            avroReader = new DataFileReader<>(seekableInput, new SpecificDatumReader<>(schema));
        }

        if (splitOffset > 0) {
            avroReader.sync(splitOffset);
        }

        return new AvroReader<>(avroReader, splitEnd);
    }

    @Override
    public Reader<T> restoreReader(Configuration config, FSDataInputStream stream, long restoredOffset, long splitEnd, long fileLen) throws IOException {
        Reader<T> reader = createReader(config, stream, restoredOffset, splitEnd);
        // Skip records to reach restoredOffset
        for (long skipped = 0; skipped < restoredOffset && reader.read() != null; skipped++) {
            // Skip records
        }
        return reader;
    }

    @Override
    public boolean isSplittable() {
        return true;
    }

    @Override
    public TypeInformation<T> getProducedType() {
        return TypeInformation.of(type);
    }

    private static class AvroReader<T> implements Reader<T> {
        private final DataFileReader<T> avroReader;
        private final long splitEnd;

        public AvroReader(DataFileReader<T> avroReader, long splitEnd) {
            this.avroReader = avroReader;
            this.splitEnd = splitEnd;
        }

        @Override
        public T read() throws IOException {
            if (avroReader.hasNext() && avroReader.tell() < splitEnd) {
                return avroReader.next();
            }
            return null;
        }

        @Override
        public void close() throws IOException {
            avroReader.close();
        }

        @Override
        public CheckpointedPosition getCheckpointedPosition() {
            try {
                return new CheckpointedPosition(avroReader.tell(), 0L);
            } catch (IOException e) {
                return null;
            }
        }
    }

    private static class FSDataSeekableInput implements SeekableInput {
        private final FSDataInputStream stream;
        private final long splitStart;
        private final long splitLength;

        public FSDataSeekableInput(FSDataInputStream stream, long splitStart, long splitLength) throws IOException {
            this.stream = stream;
            this.splitStart = splitStart;
            this.splitLength = splitLength;
            stream.seek(splitStart);
        }

        @Override
        public void seek(long position) throws IOException {
            if (position >= splitStart && position < splitStart + splitLength) {
                stream.seek(position);
            } else {
                throw new IOException("Seek position out of split range.");
            }
        }

        @Override
        public long tell() throws IOException {
            return stream.getPos();
        }

        @Override
        public long length() {
            return splitLength;
        }

        @Override
        public int read(byte[] bytes, int off, int len) throws IOException {
            return stream.read(bytes, off, len);
        }

        @Override
        public void close() throws IOException {
            stream.close();
        }
    }
}