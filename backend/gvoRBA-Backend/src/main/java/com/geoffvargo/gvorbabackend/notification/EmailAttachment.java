package com.geoffvargo.gvorbabackend.notification;

public record EmailAttachment(String filename, byte[] content, String contentType) {}
