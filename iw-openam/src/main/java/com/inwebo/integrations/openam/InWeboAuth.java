package com.inwebo.integrations.openam;

import com.inwebo.integrations.auth.InWeboRestAuthenticator;
import com.sun.identity.authentication.spi.AMLoginModule;
import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.authentication.spi.InvalidPasswordException;
import com.sun.identity.shared.debug.Debug;
import org.apache.commons.lang.StringUtils;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.login.LoginException;
import javax.servlet.http.HttpServletRequest;
import java.security.Principal;
import java.util.Map;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.logging.Logger;

import static com.inwebo.integrations.auth.Property.*;
import static com.sun.identity.authentication.util.ISAuthConstants.LOGIN_SUCCEED;
import static com.sun.identity.shared.datastruct.CollectionHelper.getIntMapAttr;
import static com.sun.identity.shared.datastruct.CollectionHelper.getMapAttr;

public class InWeboAuth extends AMLoginModule {

  // Name for the debug-log
  private final static String DEBUG_NAME = "InWeboAuth";
  private final static Debug DEBUG = Debug.getInstance(DEBUG_NAME);
  private final static Logger LOGGER = Logger.getLogger(DEBUG_NAME);

  // Name of the resource bundle
  private final static String amAuthInweboAuth = "amAuthInWeboAuth";

  // Orders defined in the callbacks file
  private final static int STATE_BEGIN = 1;
  private final static int STATE_AUTH = 2;
  private final static int STATE_ERROR = 3;
  private final static String ACTIVE_VALUE = "Active";
  private final static String BROWSER_AUTHENTICATOR = "BROWSER";
  private final static String PUSH_AUTHENTICATOR = "PUSH";
  private final static String INWEBO_AUTH_TYPE_PARAM_NAME = "inWeboAuthType";
  private final static String BASIC_AUTHENTICATOR = "OTP";
  private static final String INWEBO_BROWSER_ALIAS_PARAM_NAME = "inWeboBrowserAlias";
  private static final String INWEBO_PUSH_ACTIVATE_PARAM_NAME = "inWeboPushActivate";
  private static final String INWEBO_SHARED_USER_NAME_PARAM_NAME = "inWeboSharedUserName";
  private Map options;
  private ResourceBundle bundle;
  private InWeboRestAuthenticator inWeboRestAuthenticator;
  private String sharedUserName;
  private String browserAlias;
  private boolean isBrowserAuthenticate;
  private boolean isPushAuthenticate;
  private Map sharedState;

  public InWeboAuth() {
    super();
  }

  @Override
  public void init(final Subject subject, final Map sharedState, final Map options) {
    if (DEBUG.messageEnabled()) {
      DEBUG.message("InWeboAuthModule::init");
    }
    try {
      this.options = options;
      this.sharedState = sharedState;
      this.bundle = amCache.getResBundle(amAuthInweboAuth, getLoginLocale());
      final Properties property = new Properties();
      property.setProperty(BASE_URL.key(), getMapAttr(options, "iplanet-am-auth-inwebo-base-url"));
      property.setProperty(SERVICE_ID.key(), String.valueOf(getIntMapAttr(options, "iplanet-am-auth-inwebo-service-id", 0, DEBUG)));
      property.setProperty(CERTIFICATE_PATH.key(), getMapAttr(options, "iplanet-am-auth-inwebo-certificate-path"));
      property.setProperty(CERTIFICATE_PASSWORD.key(), getMapAttr(options, "iplanet-am-auth-inwebo-certificate-password"));
      final String proxyUri = getMapAttr(options, "iplanet-am-auth-inwebo-proxy-url");
      final String proxyUserName = getMapAttr(options, "iplanet-am-auth-inwebo-proxy-username");
      final String proxyPassword = getMapAttr(options, "iplanet-am-auth-inwebo-proxy-password");
      if (StringUtils.isNotBlank(proxyUri)) {
        property.setProperty(PROXY_URI.key(), proxyUri);
        if (StringUtils.isNotBlank(proxyUserName) && StringUtils.isNotBlank(proxyPassword)) {
          property.setProperty(PROXY_USER.key(), proxyUserName);
          property.setProperty(PROXY_PASSWORD.key(), proxyPassword);
        }
      }
      this.inWeboRestAuthenticator = new InWeboRestAuthenticator(property, LOGGER);
      this.isBrowserAuthenticate = getBooleanMapAttr("iplanet-am-auth-inwebo-browser-authenticator");
      this.isPushAuthenticate = getBooleanMapAttr("iplanet-am-auth-inwebo-push-authenticator");
      if (isBrowserAuthenticate || isPushAuthenticate) {
        this.browserAlias = getMapAttr(options, "iplanet-am-auth-inwebo-browser-alias");
      }
    } catch (final Exception e) {
      if (DEBUG.errorEnabled()) {
        DEBUG.error("InWeboAuthModule::init - Internal Error", e);
      }
    }
  }

  @Override
  public int process(final Callback[] callbacks, final int state) throws LoginException {
    if (DEBUG.messageEnabled()) {
      DEBUG.message("InWeboAuthModule::process - state: '{}'", state);
    }
    setRequestForLegacyUI();
    switch (state) {
      case STATE_BEGIN:
        substituteUIStrings();
        return STATE_AUTH;
      case STATE_AUTH:
        return authenticateExtendedValidator(callbacks);
      case STATE_ERROR:
        setErrorText("inwebo.i18n.ui.error-1");
        return STATE_ERROR;
      default:
        throw new AuthLoginException("InWeboAuthModule::process - invalid state");
    }
  }

  @Override
  public Principal getPrincipal() {
    return new InWeboAuthPrincipal(this.sharedUserName);
  }

  private void setRequestForLegacyUI() {
    final HttpServletRequest request = this.getHttpServletRequest();
    final String inWeboAuthType = String.valueOf(request.getAttribute(INWEBO_AUTH_TYPE_PARAM_NAME));
    if (inWeboAuthType == null || "null".equals(inWeboAuthType)) {
      if (isBrowserAuthenticate) {
        request.setAttribute(INWEBO_AUTH_TYPE_PARAM_NAME, BROWSER_AUTHENTICATOR);
        request.setAttribute(INWEBO_BROWSER_ALIAS_PARAM_NAME, browserAlias);
        request.setAttribute(INWEBO_PUSH_ACTIVATE_PARAM_NAME, String.valueOf(isPushAuthenticate));
      } else if (isPushAuthenticate) {
        request.setAttribute(INWEBO_AUTH_TYPE_PARAM_NAME, PUSH_AUTHENTICATOR);
        request.setAttribute(INWEBO_BROWSER_ALIAS_PARAM_NAME, browserAlias);
      }
    }
    final String userName = getSharedUserName();
    if (StringUtils.isNotBlank(userName)) {
      request.setAttribute(INWEBO_SHARED_USER_NAME_PARAM_NAME, userName);
    }
  }

  private boolean getBooleanMapAttr(final String key) {
    final String value = getMapAttr(options, key, ACTIVE_VALUE);
    return ACTIVE_VALUE.equalsIgnoreCase(value);
  }

  private void setErrorText(final String err) throws AuthLoginException {
    // Receive correct string from properties and substitute the header in callbacks order 3.
    substituteHeader(STATE_ERROR, bundle.getString(err));
  }

  private void substituteUIStrings() throws AuthLoginException {
    substituteHeader(STATE_AUTH, bundle.getString("inwebo.i18n.ui.login.header"));
    final String userName = getSharedUserName();
    if (DEBUG.messageEnabled()) {
      DEBUG.message("InWeboAuthModule::begin - sharedUserName '{}'", userName);
    }
    final NameCallback userNameCallBack = new NameCallback(bundle.getString("inwebo.i18n.ui.login.username.prompt"));
    if (StringUtils.isNotBlank(userName)) {
      userNameCallBack.setName(userName);
    }
    replaceCallback(STATE_AUTH, 0, userNameCallBack);
    replaceCallback(STATE_AUTH, 1, new PasswordCallback(bundle.getString("inwebo.i18n.ui.login.password.prompt"), false));
    if (isBrowserAuthenticate) {
      replaceCallback(STATE_AUTH, 2, new NameCallback(BROWSER_AUTHENTICATOR));
      replaceCallback(STATE_AUTH, 3, new NameCallback(browserAlias));
      replaceCallback(STATE_AUTH, 4, new NameCallback(String.valueOf(isPushAuthenticate)));
    } else if (isPushAuthenticate) {
      replaceCallback(STATE_AUTH, 2, new NameCallback(PUSH_AUTHENTICATOR));
      replaceCallback(STATE_AUTH, 3, new NameCallback(browserAlias));
    } else {
      replaceCallback(STATE_AUTH, 2, new NameCallback(BASIC_AUTHENTICATOR));
    }
  }

  private String getSharedUserName() {
    return String.class.cast(sharedState.get(getUserKey()));
  }

  private int authenticateExtendedValidator(final Callback[] callbacks) throws InvalidPasswordException {
    // Get data from callbacks. Refer to callbacks XML file.
    final NameCallback nc = NameCallback.class.cast(callbacks[0]);
    final PasswordCallback pc = PasswordCallback.class.cast(callbacks[1]);
    this.sharedUserName = getUserName(nc);
    final String password = new String(pc.getPassword());
    final boolean result = inWeboRestAuthenticator.authorize(sharedUserName, password);
    if (result) {
      return LOGIN_SUCCEED;
    } else {
      throw new InvalidPasswordException("Access Denied", sharedUserName);
    }
  }

  private String getUserName(final NameCallback nc) {
    String userName = getSharedUserName();
    if (userName == null || userName.isEmpty()) {
      userName = nc.getName();
    }
    return userName;
  }
}