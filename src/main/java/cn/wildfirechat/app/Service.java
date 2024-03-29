package cn.wildfirechat.app;


import cn.wildfirechat.app.jpa.FavoriteItem;
import cn.wildfirechat.app.pojo.*;
import cn.wildfirechat.pojos.InputCreateDevice;
import org.springframework.web.multipart.MultipartFile;

public interface Service {
    RestResult sendCode(String mobile);
    RestResult login(String mobile, String code, String clientId, int platform);
    RestResult login1(String clientId,String username,String password,String ip);

    RestResult createPcSession(CreateSessionRequest request);
    RestResult loginWithSession(String token);

    RestResult scanPc(String token);
    RestResult confirmPc(ConfirmSessionRequest request);
    RestResult cancelPc(CancelSessionRequest request);

    RestResult changeName(String newName);

    RestResult putGroupAnnouncement(GroupAnnouncementPojo request);
    RestResult getGroupAnnouncement(String groupId);

    RestResult saveUserLogs(String userId, MultipartFile file);

    RestResult addDevice(InputCreateDevice createDevice);
    RestResult getDeviceList();
    RestResult delDevice(InputCreateDevice createDevice);

    RestResult sendMessage(SendMessageRequest request);
    RestResult uploadMedia(int mediaType, MultipartFile file);

    RestResult putFavoriteItem(FavoriteItem request);
    RestResult removeFavoriteItems(long id);
    RestResult getFavoriteItems(long id, int count);

    RestResult register(String mobile,String clientId,String username,String password ,String promoteCode) throws Exception;
    public void sendTextMessage(String fromUser, String toUser, String text);
}
