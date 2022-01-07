function setCookie(cName, cValue, expDays) {
    const d = new Date();
    d.setTime(d.getTime() + (expDays * 24 * 60 * 60 * 1000));
    let expires = "expires=" + d.toUTCString();
    document.cookie = cName + "=" + cValue + ";" + expires + ";path=/";
}

function getCookie(cname) {
    let name = cname + "=";
    let decodedCookie = decodeURIComponent(document.cookie);
    let ca = decodedCookie.split(';');
    for (let i = 0; i < ca.length; i++) {
        let c = ca[i];
        while (c.charAt(0) === ' ') {
            c = c.substring(1);
        }
        if (c.indexOf(name) === 0) {
            return c.substring(name.length, c.length);
        }
    }
    return "";
}

async function checkCookie() {
    let uuid = getCookie("_uuid");
    try {
        const response = await axios.post('/fair/login', new URLSearchParams({uuid: uuid}));
        if (response.status === 201) {
            if (response.data.uuid) {
                uuid = response.data.uuid;
                setCookie("_uuid", uuid, 365 * 5);
            }
        }
    } catch (err) {
        console.error(err)
    }
}

async function importCookie() {
    var newUUID = prompt("Paste your ID into here:");
    try {
        // Check if cookies are valid
        const response = await axios.post('/fair/login', new URLSearchParams({uuid: newUUID}));
        if (response.status === 200 && response.data.uuid) {
            uuid = response.data.uuid;
            setCookie("_uuid", uuid, 365 * 5);
            // Relaod the page for the new cookies to take place
            location.reload();
        }

    } catch (err) {
        alert("Invalid ID!")
        console.error(err)
    }
}

async function exportCookie() {
    // Copy the text inside the text field
    await navigator.clipboard.writeText(getCookie("_uuid"));

    // Alert the copied text
    alert("Copied your ID to your clipboard! (don't lose it or give it away!)");
}