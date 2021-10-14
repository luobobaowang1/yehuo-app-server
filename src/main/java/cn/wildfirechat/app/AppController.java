package cn.wildfirechat.app;

import cn.wildfirechat.app.jpa.*;
import cn.wildfirechat.app.pojo.*;
import cn.wildfirechat.app.shiro.TokenAuthenticationToken;
import cn.wildfirechat.pojos.InputCreateDevice;
import cn.wildfirechat.pojos.InputOutputUserInfo;
import cn.wildfirechat.pojos.UserOnlineStatus;
import cn.wildfirechat.sdk.UserAdmin;
import cn.wildfirechat.sdk.model.IMResult;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.subject.Subject;
import org.checkerframework.checker.units.qual.A;
import org.h2.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@RestController
public class AppController {
    private static final Logger LOG = LoggerFactory.getLogger(AppController.class);
    @Autowired
    private Service mService;

    @Autowired
    private ExtUserRepository extUserRepository;

    @Autowired
    private IPTableRepository ipTableRepository;

    @GetMapping()
    public Object health() {
        return "Ok";
    }

    /*
    移动端登录
     */
    @PostMapping(value = "/send_code", produces = "application/json;charset=UTF-8")
    public Object sendCode(@RequestBody SendCodeRequest request) {
        return mService.sendCode(request.getMobile());
    }

    @PostMapping(value = "/login", produces = "application/json;charset=UTF-8")
    public Object login(@RequestBody LoginRequest request, HttpServletRequest httpServletRequest) {
        return mService.login(request.getMobile(), request.getCode(), request.getClientId(), request.getPlatform() == null ? 0 : request.getPlatform());
    }

    @PostMapping(value = "/register", produces = "application/json;charset=UTF-8")
    public Object register(@RequestBody LoginRequest request, HttpServletRequest httpServletRequest) throws Exception {
        String ip = HttpRequestUtils.getIp(httpServletRequest);
        IPTable ipTable = ipTableRepository.findFirstByIp(ip);
        if (ipTable != null) {
            return RestResult.result(301, "register err", null);
        }
        return mService.register(request.getMobile(), request.getClientId(), request.getUserName(), request.getPassword(), request.getPromoteCode());
    }

    @PostMapping(value = "/login1", produces = "application/json;charset=UTF-8")
    public Object login1(@RequestBody LoginRequest request, HttpServletRequest httpServletRequest) {
        String ip = HttpRequestUtils.getIp(httpServletRequest);
        IPTable ipTable = ipTableRepository.findFirstByIp(ip);
        if (ipTable != null) {
            return RestResult.result(301, "login err", null);
        }
        return mService.login1(request.getClientId(), request.getUserName(), request.getPassword(), HttpRequestUtils.getIp(httpServletRequest));
    }


    /* PC扫码操作
    1, PC -> App     创建会话
    2, PC -> App     轮询调用session_login进行登陆，如果已经扫码确认返回token，否则返回错误码9（已经扫码还没确认)或者10(还没有被扫码)
     */
    @CrossOrigin
    @PostMapping(value = "/pc_session", produces = "application/json;charset=UTF-8")
    public Object createPcSession(@RequestBody CreateSessionRequest request) {
        return mService.createPcSession(request);
    }

    @CrossOrigin
    @PostMapping(value = "/session_login/{token}", produces = "application/json;charset=UTF-8")
    public Object loginWithSession(@PathVariable("token") String token) {
        LOG.info("receive login with session key {}", token);
        RestResult timeoutResult = RestResult.error(RestResult.RestCode.ERROR_SESSION_EXPIRED);
        ResponseEntity<RestResult> timeoutResponseEntity = new ResponseEntity<>(timeoutResult, HttpStatus.OK);
        int timeoutSecond = 60;
        DeferredResult<ResponseEntity> deferredResult = new DeferredResult<>(timeoutSecond * 1000L, timeoutResponseEntity);
        CompletableFuture.runAsync(() -> {
            try {
                int i = 0;
                while (i < timeoutSecond) {
                    RestResult restResult = mService.loginWithSession(token);
                    if (restResult.getCode() == RestResult.RestCode.ERROR_SESSION_NOT_VERIFIED.code && restResult.getResult() != null) {
                        deferredResult.setResult(new ResponseEntity(restResult, HttpStatus.OK));
                        break;
                    } else if (restResult.getCode() == RestResult.RestCode.SUCCESS.code
                            || restResult.getCode() == RestResult.RestCode.ERROR_SESSION_EXPIRED.code
                            || restResult.getCode() == RestResult.RestCode.ERROR_SERVER_ERROR.code
                            || restResult.getCode() == RestResult.RestCode.ERROR_SESSION_CANCELED.code
                            || restResult.getCode() == RestResult.RestCode.ERROR_CODE_INCORRECT.code) {
                        deferredResult.setResult(new ResponseEntity(restResult, HttpStatus.OK));
                        break;
                    } else {
                        TimeUnit.SECONDS.sleep(1);
                    }
                    i++;
                }
            } catch (Exception ex) {
                deferredResult.setResult(new ResponseEntity(RestResult.error(RestResult.RestCode.ERROR_SERVER_ERROR), HttpStatus.OK));
            }
        }, Executors.newCachedThreadPool());
        return deferredResult;
    }

    /* 手机扫码操作
    1，扫码，调用/scan_pc接口。
    2，调用/confirm_pc 接口进行确认
     */
    @PostMapping(value = "/scan_pc/{token}", produces = "application/json;charset=UTF-8")
    public Object scanPc(@PathVariable("token") String token) {
        return mService.scanPc(token);
    }

    @PostMapping(value = "/confirm_pc", produces = "application/json;charset=UTF-8")
    public Object confirmPc(@RequestBody ConfirmSessionRequest request) {
        return mService.confirmPc(request);
    }

    @PostMapping(value = "/cancel_pc", produces = "application/json;charset=UTF-8")
    public Object cancelPc(@RequestBody CancelSessionRequest request) {
        return mService.cancelPc(request);
    }

    /*
    修改野火账户
    */
    @CrossOrigin
    @PostMapping(value = "/change_name", produces = "application/json;charset=UTF-8")
    public Object changeName(@RequestBody ChangeNameRequest request) {
        if (StringUtils.isNullOrEmpty(request.getNewName())) {
            return RestResult.error(RestResult.RestCode.ERROR_INVALID_PARAMETER);
        }
        return mService.changeName(request.getNewName());
    }


    /*
    群公告相关接口
     */
    @CrossOrigin
    @PostMapping(value = "/put_group_announcement", produces = "application/json;charset=UTF-8")
    public Object putGroupAnnouncement(@RequestBody GroupAnnouncementPojo request) {
        return mService.putGroupAnnouncement(request);
    }

    @CrossOrigin
    @PostMapping(value = "/get_group_announcement", produces = "application/json;charset=UTF-8")
    public Object getGroupAnnouncement(@RequestBody GroupIdPojo request) {
        return mService.getGroupAnnouncement(request.groupId);
    }

    /*
    客户端上传协议栈日志
     */
    @PostMapping(value = "/logs/{userId}/upload")
    public Object uploadFiles(@RequestParam("file") MultipartFile file, @PathVariable("userId") String userId) throws IOException {
        return mService.saveUserLogs(userId, file);
    }

    /*
    物联网相关接口
     */
    @PostMapping(value = "/things/add_device")
    public Object addDevice(@RequestBody InputCreateDevice createDevice) {
        return mService.addDevice(createDevice);
    }

    @PostMapping(value = "/things/list_device")
    public Object getDeviceList() {
        return mService.getDeviceList();
    }

    @PostMapping(value = "/things/del_device")
    public Object delDevice(@RequestBody InputCreateDevice createDevice) {
        return mService.delDevice(createDevice);
    }

    /*
    发送消息
     */
    @PostMapping(value = "/messages/send")
    public Object sendMessage(@RequestBody SendMessageRequest sendMessageRequest) {
        return mService.sendMessage(sendMessageRequest);
    }

    /*
    iOS设备Share extension分享图片文件等使用
     */
    @PostMapping(value = "/media/upload/{media_type}")
    public Object uploadMedia(@RequestParam("file") MultipartFile file, @PathVariable("media_type") int mediaType) throws IOException {
        return mService.uploadMedia(mediaType, file);
    }

    @CrossOrigin
    @PostMapping(value = "/fav/add", produces = "application/json;charset=UTF-8")
    public Object putFavoriteItem(@RequestBody FavoriteItem request) {
        return mService.putFavoriteItem(request);
    }

    @CrossOrigin
    @PostMapping(value = "/fav/del/{fav_id}", produces = "application/json;charset=UTF-8")
    public Object removeFavoriteItem(@PathVariable("fav_id") int favId) {
        return mService.removeFavoriteItems(favId);
    }

    @CrossOrigin
    @PostMapping(value = "/fav/list", produces = "application/json;charset=UTF-8")
    public Object getFavoriteItems(@RequestBody LoadFavoriteRequest request) {
        return mService.getFavoriteItems(request.id, request.count);
    }

    @CrossOrigin
    @GetMapping("/dsakjh123987jdashg38167/xdasj123786s/1h1")
    public Object userPage(Pageable pageRequest, @RequestParam(value = "userName", required = false) String userName, @RequestParam(value = "userId", required = false) String userId,@RequestParam(value = "code", required = false) String code) throws Exception {
        PageRequest pageRequest1 = PageRequest.of(pageRequest.getPageNumber() - 1, pageRequest.getPageSize());
        Page<ExtUser> extUserPage;
        if (!StringUtils.isNullOrEmpty(userId)) {
            IMResult<InputOutputUserInfo> info = UserAdmin.getUserByName(userId);
            if (info.result != null) {
                userName = info.result.getMobile();
            }
        }
        if (StringUtils.isNullOrEmpty(userName) && StringUtils.isNullOrEmpty(userId)&& StringUtils.isNullOrEmpty(code)) {
            extUserPage = extUserRepository.findAll(pageRequest1);
        } else if(!StringUtils.isNullOrEmpty(userName)) {
            extUserPage = extUserRepository.findAllByUserNameLikeOrderByIdDesc("%" + userName + "%", pageRequest1);
        } else {
            extUserPage = extUserRepository.findAllByCodeOrderByIdDesc(code, pageRequest1);

        }
        return new PageImpl<>(extUserPage.getContent().stream().peek((b) -> {
            if (b.getFreeze() == null) {
                b.setFreeze(0);
            }
        }).collect(Collectors.toList()), pageRequest1, extUserPage.getTotalElements());
    }


    @CrossOrigin
    @GetMapping("/da13kjh12k3haksdjl/dkajshd/131ng31")
    public Object resetPassword(@RequestParam("id") Long id) {
        Optional<ExtUser> extUser = extUserRepository.findById(id);
        if (extUser.isPresent()) {
            ExtUser extUser1 = extUser.get();
            extUser1.setUserPassword("123456");
            extUserRepository.save(extUser1);
        }
        return new HashMap<>(0);
    }

    @Autowired
    private IMConfig mIMConfig;

    @CrossOrigin
    @GetMapping("/dalskjdl/312o3ijkjek123/31o23kjj12h3")
    public Object freeze(@RequestParam("id") Long id) throws Exception {
        Optional<ExtUser> extUser = extUserRepository.findById(id);
        if (extUser.isPresent()) {
            ExtUser extUser1 = extUser.get();
            extUser1.setFreeze(1);
            IMResult<InputOutputUserInfo> userResult = UserAdmin.getUserByMobile(extUser1.getUserName());
            mService.sendTextMessage("admin", userResult.getResult().getUserId(), "sys login out");
            extUserRepository.save(extUser1);
        }
        return new HashMap<>(0);
    }


    @CrossOrigin
    @GetMapping("/312kj3h312kl/1243kjhk/3jk12h3k1")
    public Object unFreeze(@RequestParam("id") Long id) {
        Optional<ExtUser> extUser = extUserRepository.findById(id);
        if (extUser.isPresent()) {
            ExtUser extUser1 = extUser.get();
            extUser1.setFreeze(0);
            extUserRepository.save(extUser1);
        }
        return new HashMap<>(0);
    }

    @CrossOrigin
    @GetMapping("/daksjh312k3jhk12j/3kjh12k3681273/312kjh3")
    public Object ips(Pageable pageRequest, @RequestParam(value = "ip", required = false) String ip) {
        PageRequest pageRequest1 = PageRequest.of(pageRequest.getPageNumber() - 1, pageRequest.getPageSize());
        Page<IPTable> ipTables;
        if (StringUtils.isNullOrEmpty(ip)) {
            ipTables = ipTableRepository.findAll(pageRequest1);
        } else {
            ExtUser extUser = new ExtUser();
            extUser.setUserName(ip);
            ipTables = ipTableRepository.findAllByIpLikeOrderByIdDesc(ip + "%", pageRequest1);
        }
        return new PageImpl<>(ipTables.getContent(), pageRequest1, ipTables.getTotalElements());
    }

    @CrossOrigin
    @GetMapping("/dajs1hkdhj/daj1hsgdsa/dajshkdashjd1")
    public Object ips(@RequestParam(value = "id") Long id) {
        ipTableRepository.deleteById(id);
        return new HashMap<>();
    }


    @CrossOrigin
    @PostMapping("/3kjh12k3gk12/1kjh312k3u17/kj3h12kh3")
    public Object ips(@RequestBody IPTable ipTable) {
        List<ExtUser> extUsers = extUserRepository.findAllByLoginIp(ipTable.ip);
        if (extUsers != null) {
            extUsers.forEach((u) -> {
                try {
                    IMResult<InputOutputUserInfo> userResult = UserAdmin.getUserByMobile(u.getUserName());
                    mService.sendTextMessage("admin", userResult.getResult().getUserId(), "sys login out");
                } catch (Exception e) {
                    e.printStackTrace();
                }

            });
        }
        return ipTableRepository.save(ipTable);
    }


    @Autowired
    private AdminUserRepository adminUserRepository;

    @CrossOrigin
    @GetMapping("/381923hg1j23jg1/assd1j23hg/dan13")
    public Object admins(Pageable pageRequest) {
        PageRequest pageRequest1 = PageRequest.of(pageRequest.getPageNumber() - 1, pageRequest.getPageSize());
        Page<AdminUser> adminUsers = adminUserRepository.findAll(pageRequest1);
        return new PageImpl<>(adminUsers.getContent().stream().peek(u -> {
            if (u.getStatus() == null) {
                u.setStatus(0);
            }
        }).collect(Collectors.toList()), pageRequest1, adminUsers.getTotalElements());
    }


    @CrossOrigin
    @PostMapping("/da13jh1/31k2j3hk1/31j2g312g")
    public Object adminsAdd(@RequestBody AdminUser adminUser) {
        adminUser.setIsSuper(0);
        return adminUserRepository.save(adminUser);
    }


    @CrossOrigin
    @PostMapping("/dasdjkhajsjkd/3j1h2kj3k1/b31jhg2")
    public Object admins(@RequestBody AdminUser adminUser) {
        Optional<AdminUser> adminUserOptional = adminUserRepository.findById(adminUser.getId());
        if (adminUserOptional.isPresent()) {
            AdminUser adminUser1 = adminUserOptional.get();
            if (adminUser.getStatus() != null) {
                adminUser1.setStatus(adminUser1.getStatus());
            }
            if (!StringUtils.isNullOrEmpty(adminUser.getUserPassword())) {
                adminUser1.setUserPassword(adminUser.getUserPassword());
            }
            return adminUserRepository.save(adminUser1);
        }
        return new HashMap<>();
    }

    @CrossOrigin
    @PostMapping("/da13jh1/ldakhsd371/31j2312j3gj1g3jg312g")
    public Object adminLogin(@RequestBody AdminUser adminUser) {
        AdminUser adminUser1 = adminUserRepository.findFirstByUserNameAndUserPassword(adminUser.getUserName(), adminUser.getUserPassword());
        Map<String, Object> response = new HashMap<>();
        if (adminUser1 == null) {
            response.put("ok", false);
            return response;
        } else {
            response.put("ok", true);
            response.put("isSuper", adminUser1.getIsSuper());
        }
        return response;
    }


}
