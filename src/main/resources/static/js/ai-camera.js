window.TeachingAiCamera = window.TeachingAiCamera || {};

window.TeachingAiCamera.startCamera = async function startCamera(options) {
    console.log('进入摄像头方法', options);
    const video = options.video;
    const placeholder = options.placeholder;
    const onMessage = options.onMessage || function () {};
    if (!video) {
        onMessage('未找到摄像头预览区域。');
        return null;
    }
    if (!navigator.mediaDevices || !navigator.mediaDevices.getUserMedia) {
        onMessage('当前浏览器不支持摄像头调用，请使用新版 Chrome 或 Edge。');
        return null;
    }
    const host = window.location.hostname;
    const isLocalhost = host === 'localhost' || host === '127.0.0.1' || host === '[::1]';
    if (!window.isSecureContext && !isLocalhost) {
        onMessage('浏览器要求 HTTPS 或 localhost 才能开启摄像头。请用 http://localhost:端口 访问，不要用局域网 IP。');
        return null;
    }

    const withAudio = options.audio !== false;
    const audioConstraints = withAudio ? { echoCancellation: true, noiseSuppression: true } : false;
    const constraintsList = [
        { video: true, audio: audioConstraints },
        { video: { width: { ideal: 1280 }, height: { ideal: 720 } }, audio: audioConstraints },
        { video: { facingMode: { ideal: 'environment' }, width: { ideal: 1280 }, height: { ideal: 720 } }, audio: audioConstraints },
        { video: true, audio: false }
    ];

    let lastError = null;
    for (const constraints of constraintsList) {
        try {
            const stream = await navigator.mediaDevices.getUserMedia(constraints);
            video.srcObject = stream;
            await video.play().catch(function () {});
            if (placeholder) placeholder.classList.add('d-none');
            const hasAudio = stream.getAudioTracks && stream.getAudioTracks().length > 0;
            onMessage(hasAudio ? '摄像头和麦克风已开启。' : '摄像头已开启，麦克风未开启或不可用。');
            return stream;
        } catch (error) {
            lastError = error;
        }
    }

    const name = lastError && lastError.name ? lastError.name : '';
    if (name === 'NotAllowedError' || name === 'PermissionDeniedError') {
        onMessage('摄像头权限被拒绝，请在浏览器地址栏允许摄像头权限后刷新重试。');
    } else if (name === 'NotFoundError' || name === 'DevicesNotFoundError') {
        onMessage('没有检测到可用摄像头，请检查设备连接。');
    } else if (name === 'NotReadableError' || name === 'TrackStartError') {
        onMessage('摄像头可能被其他软件占用，请关闭占用摄像头的程序后重试。');
    } else {
        onMessage('无法开启摄像头，请检查浏览器权限、HTTPS/localhost 访问方式或设备占用。');
    }
    return null;
};

window.TeachingAiCamera.stopCamera = function stopCamera(stream, video, placeholder) {
    if (stream) {
        stream.getTracks().forEach(function (track) { track.stop(); });
    }
    if (video) video.srcObject = null;
    if (placeholder) placeholder.classList.remove('d-none');
};
