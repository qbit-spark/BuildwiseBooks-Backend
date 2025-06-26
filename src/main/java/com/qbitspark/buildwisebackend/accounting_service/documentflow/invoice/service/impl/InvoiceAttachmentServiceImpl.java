package com.qbitspark.buildwisebackend.accounting_service.documentflow.invoice.service.impl;

import com.qbitspark.buildwisebackend.accounting_service.documentflow.invoice.entity.InvoiceAttachmentEntity;
import com.qbitspark.buildwisebackend.accounting_service.documentflow.invoice.entity.InvoiceDocEntity;
import com.qbitspark.buildwisebackend.accounting_service.documentflow.invoice.repo.InvoiceAttachmentRepo;
import com.qbitspark.buildwisebackend.accounting_service.documentflow.invoice.service.InvoiceAttachmentService;
import com.qbitspark.buildwisebackend.globe_api_client.filebox_client.services.FileBoxService;
import com.qbitspark.buildwisebackend.globe_api_client.payloads.FileBoxFileResponse;
import com.qbitspark.buildwisebackend.globe_api_client.payloads.FileBoxFolderContentsResponse;
import com.qbitspark.buildwisebackend.globe_api_client.payloads.FileBoxFolderResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class InvoiceAttachmentServiceImpl implements InvoiceAttachmentService {

    private final FileBoxService fileBoxService;
    private final InvoiceAttachmentRepo attachmentRepository;

    @Value("${app.filebox.root-folder}")
    private String rootFolderName;

    private UUID rootFolderId; // Cache for root folder ID

}