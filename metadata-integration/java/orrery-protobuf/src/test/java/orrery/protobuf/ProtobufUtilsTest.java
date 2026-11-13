package orrery.protobuf;

import static orrery.protobuf.TestFixtures.getTestProtobufFileSet;
import static orrery.protobuf.TestFixtures.getTestProtoc;
import static org.testng.Assert.*;

import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.ExtensionRegistry;
import orrery.protobuf.model.ProtobufGraph;
import java.io.IOException;
import java.util.Arrays;
import org.testng.annotations.Test;

public class ProtobufUtilsTest {

  @Test
  public void registryTest() throws IOException, IllegalArgumentException {
    // Test that primitive and enum-type extensions are properly handled
    byte[] protocBytes = getTestProtoc("extended_protobuf", "messageA").readAllBytes();
    DescriptorProtos.FileDescriptorSet fileSet =
        getTestProtobufFileSet("extended_protobuf", "messageA");
    ExtensionRegistry registry = ProtobufUtils.buildRegistry(fileSet);
    DescriptorProtos.FileDescriptorSet fileSetWithRegistry =
        DescriptorProtos.FileDescriptorSet.parseFrom(protocBytes, registry);

    assertNotEquals(fileSet, fileSetWithRegistry);

    // Without the ExtensionRegistry we get field numbers instead of the names.
    ProtobufGraph graph = new ProtobufGraph(fileSet, null);
    assertEquals(
        "[meta.msg.classification_enum]: HighlyConfidential\n"
            + "[meta.msg.team]: \"corpGroup:TeamB\"\n"
            + "[meta.msg.team]: \"corpUser:orrery\"\n"
            + "[meta.msg.technical_owner]: \"corpGroup:TechnicalOwner\"\n"
            + "[meta.msg.domain]: \"Engineering\"\n"
            + "[meta.msg.type]: ENTITY\n"
            + "[meta.msg.bool_feature]: true\n"
            + "[meta.msg.alert_channel]: \"#alerts\"\n"
            + "[meta.msg.tag_list]: \"a, b, c\"\n"
            + "[meta.msg.repeat_string]: \"a\"\n"
            + "[meta.msg.repeat_string]: \"b\"\n"
            + "[meta.msg.repeat_enum]: ENTITY\n"
            + "[meta.msg.repeat_enum]: EVENT\n",
        graph.root().messageProto().getOptions().toString());
  }

  @Test
  public void messageTypeExtensionRegistryTest() throws IOException {
    // Test that MESSAGE-type extensions (not just primitive/enum) are properly handled
    byte[] protocBytes = getTestProtoc("extended_protobuf", "messageF").readAllBytes();
    DescriptorProtos.FileDescriptorSet fileSet =
        getTestProtobufFileSet("extended_protobuf", "messageF");
    ExtensionRegistry registry = ProtobufUtils.buildRegistry(fileSet);
    DescriptorProtos.FileDescriptorSet fileSetWithRegistry =
        DescriptorProtos.FileDescriptorSet.parseFrom(protocBytes, registry);

    assertNotEquals(fileSet, fileSetWithRegistry);

    // Verify that message-type extension options are properly parsed
    ProtobufGraph graph = new ProtobufGraph(fileSetWithRegistry, "extended_protobuf.Product");
    String options = graph.root().messageProto().getOptions().toString();

    // The extension should be properly parsed with field names (not field numbers)
    assertTrue(
        options.contains("[meta.nested_msg.metadata]"),
        "Expected MESSAGE-type extension to be properly registered");
    assertTrue(
        options.contains("description: \"Product catalog message\""),
        "Expected extension field values to be parsed");
    assertTrue(options.contains("priority: 10"), "Expected extension field values to be parsed");
  }

  @Test
  public void testCollapseLocationCommentsWithUTF8() {
    DescriptorProtos.SourceCodeInfo.Location location =
        DescriptorProtos.SourceCodeInfo.Location.newBuilder()
            .addAllLeadingDetachedComments(Arrays.asList("/* Emoji 😊 */", "/* Accented é */"))
            .setLeadingComments("/* Chinese 你好 */\n// Russian Привет")
            .setTrailingComments("// Korean 안녕")
            .build();

    String actual = ProtobufUtils.collapseLocationComments(location);
    String expected = "Emoji 😊 */\nAccented é */\nChinese 你好 */\nRussian Привет\nKorean 안녕";

    assertEquals(expected, actual);
  }
}
