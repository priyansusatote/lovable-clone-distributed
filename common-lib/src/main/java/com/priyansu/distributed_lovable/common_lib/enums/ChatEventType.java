package com.priyansu.distributed_lovable.common_lib.enums;

public enum ChatEventType {

    THOUGHT,      //"Thought for 2s"
    MESSAGE,     // Standard Conversation text
    FILE_EDIT,   //code generation <file>
    TOOL_LOG  // "Reading file..." <tool>
}
