package com.ll.codicaster.boundedContext.image.service;

import java.io.File;
import java.io.IOException;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.FileEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.ll.codicaster.boundedContext.image.repository.ImageRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ImageService {

	@Value("${naver-cloud-platform.access-key}")
	private String ACCESS_KEY;

	@Value("${naver-cloud-platform.secret-key}")
	private String SECRET_KEY;

	@Value("${naver-cloud-platform.bucket-name}")
	private String BUCKET_NAME;

	@Value("${naver-cloud-platform.object-storage-url}")
	private String OBJECT_STORAGE_URL;

	public void uploadImage(MultipartFile file) {

		// 파일 시스템에 파일을 임시로 저장합니다.
		File fileToUpload = new File(file.getOriginalFilename());
		try {
			file.transferTo(fileToUpload);
		} catch (IOException e) {
			throw new RuntimeException("Failed to save the image file", e);
		}

		String objectName = fileToUpload.getName();

		CloseableHttpClient httpClient = HttpClients.createDefault();

		HttpPut httpPut = new HttpPut(OBJECT_STORAGE_URL + "/" + BUCKET_NAME + "/" + objectName);
		httpPut.setHeader("x-ncp-iam-access-key", ACCESS_KEY);
		httpPut.setHeader("x-ncp-apigw-timestamp", String.valueOf(System.currentTimeMillis()));
		httpPut.setHeader("x-ncp-apigw-signature-v2", SECRET_KEY); // Note: This should be your signature, not the secret key itself.

		FileEntity reqEntity = new FileEntity(fileToUpload, ContentType.create(file.getContentType()));
		httpPut.setEntity(reqEntity);

		CloseableHttpResponse response = null;
		try {
			response = httpClient.execute(httpPut);
			System.out.println("Response code: " + response.getStatusLine().getStatusCode());
			HttpEntity entity = response.getEntity();
			System.out.println("Response body: " + EntityUtils.toString(entity));
		} catch (IOException e) {
			throw new RuntimeException("Failed to upload the image", e);
		} finally {
			if (response != null) {
				try {
					response.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			// 업로드가 완료되면 임시 파일을 삭제합니다.
			fileToUpload.delete();
		}
	}
}
