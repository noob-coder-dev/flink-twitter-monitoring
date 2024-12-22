package com.yeah.flink.app;

import org.apache.avro.generic.GenericRecord;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.connector.file.src.FileSource;
import org.apache.flink.core.fs.Path;
import org.apache.flink.formats.avro.typeutils.GenericRecordAvroTypeInfo;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.avro.Schema;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

public class TwitterAvroProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(TwitterAvroProcessor.class);

    public static void main(String[] args) throws Exception{
        final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        ParameterTool params = ParameterTool.fromArgs(args);
        String inputFile = params.getRequired("input");
        String outputFile = params.get("output", "resources/input/avro");
        int parallelism = Integer.parseInt(params.get("p", "1"));
        LOG.info("Received parameters {}", params);
        try {
            LOG.debug("Run started...");
            run(env, inputFile, outputFile, parallelism);
        } catch (Exception e) {
            LOG.error("Something went wrong!", e);
            throw e;
        }
    }

    private static void run (
            StreamExecutionEnvironment env,
            String inputFileString,
            String outputFileString,
            int parallelism
    ) throws IOException {
        Schema sourceSchema = new Schema.Parser().parse(new File("/Users/manojitroy/flink-practice/flink-word-count/src/main/resources/avro/schema/twitter.avsc"));
        Path inputFilePath = new Path(inputFileString);
        GenericRecordAvroTypeInfo genericRecordAvroTypeInfo = new GenericRecordAvroTypeInfo(sourceSchema);

        FileSource<GenericRecord> fileSource = FileSource
                .forRecordStreamFormat(
                        new AvroSimpleStreamFormat<GenericRecord>(sourceSchema),
                        inputFilePath
                ).build();
        DataStream<GenericRecord> avroStream = env
                .fromSource(
                        fileSource,
                        WatermarkStrategy.noWatermarks(),
                        "Input Twitter Logs"
                ).returns(genericRecordAvroTypeInfo);
        avroStream.print();

        try {
            LOG.debug("Execution started...");
            env.execute("Twitter Avro File Processor");
            LOG.debug("Execution terminated!");
        } catch (Exception e) {
            LOG.error("File not found!", e);
            throw new RuntimeException(e);
        }
    }
}
