function logInfo(value) {
    if (window.console) {
        console.log(value);
    }
}

function hideInWeboCallBack() {
    $("div.iw3").remove();
    $("div.iw4").remove();
    $("div.iw5").remove();
}

function hideIwPassword() {
    $(".iwPassword").each(function() {
        $(this).hide();
    });
}

function showIwPassword() {
    $(".iwPassword").each(function() {
        $(this).show();
    });
}

function hideVa() {
    $(".vaContent").each(function() {
        $(this).hide();
    });
}

function hideHelium() {
    $(".heContent").each(function() {
        $(this).hide();
    });
}

function hideHeliumPush() {
    $("#inWeboPush").hide();
}

function showHeliumPush() {
    $("#inWeboPush").show();
}

function showOpenAMForm() {
    $("#loginForm").show();
}

function hideOpenAMForm() {
    $("#loginForm").hide();
}

function setInWeboAuthType(value) {
    $("input[name='inWeboAuthType']").val(value);
}

function startPushForm() {
    logInfo("Starting inWebo Push Form Authenticator");
    var userName = $("#IDToken1").val();
    $("#inWeboPushLogin").val(userName);
    hideVa();
    hideHelium();
    hideOpenAMForm();
    showHeliumPush();
}

function startOtpDisplay() {
    logInfo("Starting InWebo OTP Authenticator");
    setInWeboAuthType("OTP");
    $("#Login.Submit").attr("disabled", "disabled");
    hideVa();
    hideHelium();
    hideHeliumPush();
    showIwPassword();
    showOpenAMForm();
}

function startHeliumPush() {
    try {
        logInfo("Starting inWebo Helium Push Authenticator");
        setInWeboAuthType("BROWSER");
        hideVa();
        hideHelium();
        var userName = $("#inWeboPushLogin").val();
        if (userName === "") {
            logInfo("no entering login");
            return false;
        } else {
            logInfo("Send push notification for user '" + userName + "'");
            $("#IDToken1").val(userName);
            $("#inWeboPushConfirm").attr("disabled", "disabled");
            $("#Login.Submit").attr("disabled", "disabled");
            hideHeliumPush();
            hideIwPassword();
            showOpenAMForm();
            $("#inwebo_login").text(userName);
            $("#heliumAuthenticate").attr("action", "push_authenticate");
            $("#heliumDesign").attr("container", "helium_push");
            start_helium("heliumAuthenticate", function (iw, data) {
                if (data.result === "ok") {
                    logInfo("inWebo Helium Push Authenticator activated");
                    $("#Login.Submit").removeAttr("disabled");
                    iw.insertFields(data);
                } else {
                    startOtpDisplay();
                }
            });
        }
    } catch (e) {
         logInfo("Error: could not start inWebo Helium Push in this browser. " + e.toString());
         startOtpDisplay();
    }
}

function startHelium(canPush) {
    try {
        logInfo("Starting inWebo Helium Authenticator");
        hideVa();
        setInWeboAuthType("BROWSER");
        start_helium("heliumAuthenticate", function(iw, data) {
            if (data.result === "ok") {
                logInfo("inWebo Helium Authenticator activated");
                iw.insertFields(data);
            } else if (data.result === "nok" && data.error === "no_profile" && canPush === "true") {
                startPushForm();
            } else {
                logInfo("inWebo Helium Authenticator is not activated");
                startOtpDisplay();
            }
        });
    } catch (e) {
        logInfo("Error: could not start inWebo Helium in this browser. " + e.toString());
        startOtpDisplay();
    }
}

function startVa(canPush) {
    try {
        logInfo("Starting inWebo VA Authenticator");
        hideOpenAMForm();
        hideHeliumPush();
        hideHelium();
        setInWeboAuthType("BROWSER");
        iwstart("vaStart", function(iw, data) {
            if (data.type === "success" && data.code === "ok" && data.action === "authentication") {
                iw.insertFields(data.result);
            } else if (data.code === "info" && data.result.reason === "ui_shown") {
                hideHelium();
            } else if (data.code === "nok") {
                if (data.result.reason === "no_container") {
                    logInfo("inWebo Virtual Authenticator display container could not be found!");
                } else if (data.result.reason === "no_profile") {
                    logInfo("inWebo Virtual Authenticator no_profile");
                }
                startHelium(canPush);
            } else {
                startHelium(canPush);
            }
        });
    } catch (e) {
        logInfo("Error: could not start inWebo Virtual Authenticator in this browser. " + e.toString());
        startHelium(canPush);
    }
}