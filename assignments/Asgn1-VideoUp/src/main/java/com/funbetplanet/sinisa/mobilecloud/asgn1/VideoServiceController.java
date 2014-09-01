package com.funbetplanet.sinisa.mobilecloud.asgn1;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.magnum.dataup.VideoFileManager;
import org.magnum.dataup.model.Video;
import org.magnum.dataup.model.VideoStatus;
import org.magnum.dataup.model.VideoStatus.VideoState;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

@Controller
public class VideoServiceController {

	protected AtomicLong currentId = new AtomicLong(0L);
	protected Map<Long, Video> videos = new HashMap<Long, Video>();

	@RequestMapping(value = "/video", method = RequestMethod.GET)
	public @ResponseBody Collection<Video> getVideo() {
		return videos.values();
	}

	@RequestMapping(value = "/video", method = RequestMethod.POST)
	public @ResponseBody Video addVideo(@RequestBody Video video) {
		checkAndUpdate(video);
		videos.put(video.getId(), video);
		return video;
	}

	@RequestMapping(value = "/video/{id}/data", method = RequestMethod.POST)
	public @ResponseBody VideoStatus addVideoData(@PathVariable("id") Long id,
			@RequestParam("data") MultipartFile file,
			HttpServletResponse response) {
		VideoStatus result = null;

		Video video = videos.get(id);

		if (video == null) {
			response.setStatus(HttpServletResponse.SC_NOT_FOUND);
		} else {
			try {
				VideoFileManager.get().saveVideoData(video,
						file.getInputStream());

				result = new VideoStatus(VideoState.READY);
			} catch (IOException e) {
				response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			}
		}
		return result;
	}

	@RequestMapping(value = "/video/{id}/data", method = RequestMethod.GET)
	public void getVideoData(@PathVariable("id") Long id,
			HttpServletResponse response) {
		Video video = videos.get(id);

		if (video == null) {
			response.setStatus(HttpServletResponse.SC_NOT_FOUND);
		} else {
			try {
				VideoFileManager.get().copyVideoData(video,
						response.getOutputStream());
			} catch (IOException e) {
				response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			}
		}
	}

	private void checkAndUpdate(Video video) {
		if (video.getId() == 0) {
			long counter = currentId.incrementAndGet();
			video.setId(counter);
			video.setDataUrl(getUrlBaseForLocalServer() + "/video/" + counter
					+ "/data");
		}
	}

	private String getUrlBaseForLocalServer() {
		HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder
				.getRequestAttributes()).getRequest();
		String base = "http://"
				+ request.getServerName()
				+ ((request.getServerPort() != 80) ? ":"
						+ request.getServerPort() : "");
		return base;
	}
}
