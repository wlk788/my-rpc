package com.wlk.compress;

import com.wlk.serialize.Serializer;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CompressorWrapper {
    private byte code;
    private String type;
    private Compressor compressor;
}
