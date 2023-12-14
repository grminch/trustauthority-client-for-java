package trust_authority_client;

// Java Standard Library Imports
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;

// JNA (Java Native Access) Library Imports
import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.Library;
import com.sun.jna.Structure;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;

// Jackson JSON Library Import
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * TdxAdapter class for TDX Quote collection from TDX enabled platform
 * This class implements the base EvidenceAdapter interface.
 */
public class TdxAdapter implements EvidenceAdapter {

    private byte[] uData;
    private EventLogParser evLogParser;

    /**
     * Constructs a new TdxAdapter object with the specified uData and evLogParser.
     *
     * @param uData             uData provided by the user.
     * @param evLogParser       EventLogParser object provided by user.
     */
    public TdxAdapter(byte[] uData, EventLogParser evLogParser) {
        this.uData = uData;
        this.evLogParser = evLogParser;
    }

    /**
     * Constructs a new TdxAdapter object with the specified uData and evLogParser.
     *
     * @param uData             uData provided by the user.
     * @param evLogParser       EventLogParser object provided by user.
     * @return TdxAdapter object
     */
    public static TdxAdapter newEvidenceAdapter(byte[] uData, EventLogParser evLogParser) {
        return new TdxAdapter(uData, evLogParser);
    }

    /**
     * TdxAttestLibrary is an interface that extends JNA's Library interface.
     * It defines the methods that will be mapped to the native library functions.
     */
    public interface TdxAttestLibrary extends Library {
        // private variable to hold an instance of the native library tdx_attest interface
        TdxAttestLibrary INSTANCE = (TdxAttestLibrary) Native.load("tdx_attest", TdxAttestLibrary.class);
    
        int tdx_att_get_quote(Pointer tdxReportData, Pointer attReport, int attReportSize,
                              TdxUuid selectedAttKeyId, PointerByReference quoteBuf, IntByReference quoteSize, int flags);
    
        int tdx_att_free_quote(Pointer quoteBuf);
    }

    /**
     * Java object representing a C struct TdxUuid.
     * Extends JNA's Structure class for seamless mapping to native memory.
     */
    public static class TdxUuid extends Structure {
        // Define structure fields to match the C struct
    }

    /**
     * Helper method to convert List<RtmrEventLog> to String
     *
     * @param myList List<RtmrEventLog> object
     * @return The List<RtmrEventLog> converted to string
     */
    public static String convertListToJson(List<RtmrEventLog> myList) {
        ObjectMapper objectMapper = new ObjectMapper();

        try {
            // Convert the list to a JSON string
            return objectMapper.writeValueAsString(myList);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * collectEvidence is used to get TDX quote using DCAP Quote Generation service
     *
     * @param nonce nonce value passed by user
     * @return Evidence object containing the fetched TDX quote
     */
    public Evidence collectEvidence(byte[] nonce) throws NoSuchAlgorithmException {

        MessageDigest sha512Digest = MessageDigest.getInstance("SHA-512");
        sha512Digest.update(nonce);
        sha512Digest.update(this.uData);
        byte[] reportData = sha512Digest.digest();

        // cReportData holds the reportdata provided as input from attested app
        Memory cReportData = new Memory(reportData.length);
        cReportData.write(0, reportData, 0, reportData.length);

        // Passing this as null as it's not required
        // TdxUuid selectedAttKeyId = new TdxUuid();

        // Initialize TDX Quote objects
        IntByReference quoteSize = new IntByReference();
        PointerByReference quoteBuf = new PointerByReference();

        // Fetch TDX Quote by calling the respective tdx sdk function
        int ret = TdxAttestLibrary.INSTANCE.tdx_att_get_quote(cReportData, null, 0,
                null, quoteBuf, quoteSize, 0);
        if (ret != 0) {
            throw new RuntimeException("tdx_att_get_quote returned error code " + ret);
        }

        // Convert fetched Tdx Quote to bytes
        byte[] quote = quoteBuf.getValue().getByteArray(0, quoteSize.getValue());

        // Free TDX Quote by calling the respective tdx sdk function to avoid memory leaks
        ret = TdxAttestLibrary.INSTANCE.tdx_att_free_quote(quoteBuf.getValue());
        if (ret != 0) {
            throw new RuntimeException("tdx_att_free_quote returned error code " + ret);
        }

        // Convert List<RtmrEventLog> to Json object
        byte[] eventLog = null;
        if (this.evLogParser != null) {
            try {
                List<RtmrEventLog> rtmrEventLogs = this.evLogParser.getEventLogs();
                eventLog = convertListToJson(rtmrEventLogs).getBytes(StandardCharsets.UTF_8);
            } catch (Exception e) {
                throw new RuntimeException("Error while collecting RTMR Event Log Data", e);
            }
        }

        // Construct and return Evidence object attached with the fetched TDX Quote
        return new Evidence(1, quote, uData, eventLog);
    }
}