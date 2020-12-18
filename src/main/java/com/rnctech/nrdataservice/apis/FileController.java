package com.rnctech.nrdataservice.apis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.util.InMemoryResource;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import com.rnctech.nrdataservice.service.FileStorageService;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
/*
 * @contributor zilin
 * 2020.09
 * 
 * File upload/download controller
 */

@RestController
@RequestMapping(value = "/api/v1/fs")
public class FileController {

	private static final Logger logger = LoggerFactory.getLogger(FileController.class);

	@Autowired
	private FileStorageService fileStorageService;

	@PostMapping("/upload")
	public UploadFileResponse uploadFile(@RequestParam("file") MultipartFile file) {
		try {
			String fileName = fileStorageService.storeFile(file);

			String fileDownloadUri = ServletUriComponentsBuilder.fromCurrentContextPath().path("/file/download/")
					.path(fileName).toUriString();

			return new UploadFileResponse(fileName, fileDownloadUri, file.getContentType(), file.getSize());
		} catch (Exception e) {
			return new UploadFileResponse(file.getName(), e.getMessage());
		}
	}

	//@PostMapping("/uploadFiles")
	public List<UploadFileResponse> uploadMultipleFiles(@RequestParam("files") MultipartFile[] files) {
		return Arrays.asList(files).stream().map(file -> uploadFile(file)).collect(Collectors.toList());
	}

	
	@GetMapping(path = {"/list", "/list/{extension}"})
	public ResponseEntity<?> listFiles(@PathVariable(name = "extension", required = false) Optional<String> extension) {
		try {
			List<String> fs = fileStorageService.listUpload(extension.orElse(null));
			List<String> dlinks = fs.stream().map(f -> ServletUriComponentsBuilder.fromCurrentContextPath().path("/file/download/").path(f).toUriString()).collect(Collectors.toList());;

			return new ResponseEntity<List<String>>(dlinks, HttpStatus.OK);
		} catch (Exception e) {
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}
		
	}

	@GetMapping("/download/{fileName:.+}")
	public ResponseEntity<Resource> downloadFile(@PathVariable String fileName, HttpServletRequest request) {
		try {
			Resource resource = fileStorageService.loadFileAsResource(fileName);

			
			if(resource instanceof InMemoryResource) {
				return ResponseEntity.ok().contentType(MediaType.TEXT_PLAIN)
						.body(resource);
			}
			
			String contentType = "text/html";
			try {
				contentType = request.getServletContext().getMimeType(resource.getFile().getAbsolutePath());				
			} catch (IOException ex) {
				logger.info("Could not determine file type.");
			}

			if (contentType == null) {
				contentType = "application/octet-stream";
			}

			return ResponseEntity.ok().contentType(MediaType.parseMediaType(contentType))
					.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
					.body(resource);
		} catch (Exception e) {
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}
	}

	public class UploadFileResponse {
		private String fileName;
		private String fileDownloadUri;
		private String fileType;
		private long size;
		private String errmsg = "";

		public UploadFileResponse(String fileName, String msg) {
			this.fileName = fileName;
			this.errmsg = msg;
		}

		public UploadFileResponse(String fileName, String fileDownloadUri, String fileType, long size) {
			this.fileName = fileName;
			this.fileDownloadUri = fileDownloadUri;
			this.fileType = fileType;
			this.size = size;
		}

		public String getErrmsg() {
			return errmsg;
		}

		public void setErrmsg(String errmsg) {
			this.errmsg = errmsg;
		}

		public String getFileName() {
			return fileName;
		}

		public void setFileName(String fileName) {
			this.fileName = fileName;
		}

		public String getFileDownloadUri() {
			return fileDownloadUri;
		}

		public void setFileDownloadUri(String fileDownloadUri) {
			this.fileDownloadUri = fileDownloadUri;
		}

		public String getFileType() {
			return fileType;
		}

		public void setFileType(String fileType) {
			this.fileType = fileType;
		}

		public long getSize() {
			return size;
		}

		public void setSize(long size) {
			this.size = size;
		}

	}
}
