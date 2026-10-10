const selector = "[data-gamepad]";
const initialRepeatDelay = 400;
const repeatInterval = 120;

let activeGamepad = null;

let direction = null;
let directionStarted = 0;
let lastRepeat = 0;

let previousButtons = [];

const stickStart = 0.45;
const stickRelease = 0.2;

let stickDirection = null;

let inputLocked = true;

let selectEditing = false;
let selectOriginalIndex = -1;

const scrollDeadzone = 0.15;
const scrollSpeed = 20;

function hideCursor() {
  window.sessionStorage.setItem("gamepad-active", "1");
  document.documentElement.classList.add("gamepad-active");
}

function showCursor() {
  window.sessionStorage.removeItem("gamepad-active");
  document.documentElement.classList.remove("gamepad-active");
}

if (window.sessionStorage.getItem("gamepad-active") === "1") {
  document.documentElement.classList.add("gamepad-active");
}

function getElements() {
  const elements = [...document.querySelectorAll(selector)];

  return elements.filter((element) => {
    if (element.disabled) {
      return false;
    }

    const style = window.getComputedStyle(element);

    if (style.display === "none") {
      return false;
    }

    if (style.visibility === "hidden") {
      return false;
    }

    return element.offsetParent !== null;
  });
}

function center(rect) {
  return {
    x: rect.left + rect.width / 2,
    y: rect.top + rect.height / 2,
  };
}

function changeSelect(select, direction) {
  const options = [...select["options"]];

  let index = select.selectedIndex;
  if (index < 0) {
    index = direction > 0 ? -1 : options.length;
  }

  while (true) {
    index += direction;

    if (index < 0 || index >= options.length) {
      return;
    }

    if (!options[index].disabled) {
      break;
    }
  }

  select.selectedIndex = index;
}

function enterSelect(select) {
  selectEditing = true;
  selectOriginalIndex = select.selectedIndex;

  select.classList.add("active");
}

function submitSelect(select) {
  select.dispatchEvent(new Event("change", { bubbles: true }));

  selectEditing = false;
  selectOriginalIndex = -1;

  select.classList.remove("active");
}

function cancelSelect(select) {
  select.selectedIndex = selectOriginalIndex;
  selectEditing = false;
  selectOriginalIndex = -1;

  select.classList.remove("active");
}

function focus(element) {
  element.focus({ preventScroll: false });
}

function navigate(dir) {
  hideCursor();

  const current = document.activeElement;

  if (selectEditing && current instanceof HTMLSelectElement) {
    if (dir === "up" || dir === "left") {
      changeSelect(current, -1);
      return;
    }

    if (dir === "down" || dir === "right") {
      changeSelect(current, 1);
      return;
    }

    return;
  }

  const elements = getElements();
  const vertical = dir === "up" || dir === "down";

  if (!elements.length) {
    return;
  }

  if (!elements.includes(current)) {
    focus(elements[0]);
    return;
  }

  const rect = current.getBoundingClientRect();
  const origin = center(rect);

  let directionX = 0;
  let directionY = 0;

  switch (dir) {
    case "left":
      directionX = -1;
      break;

    case "right":
      directionX = 1;
      break;

    case "up":
      directionY = -1;
      break;

    case "down":
      directionY = 1;
      break;
  }

  const frustumCos = vertical ? Math.cos(Math.PI / 2.1) : Math.cos(Math.PI / 4);

  const candidates = [];

  for (const element of elements) {
    if (element === current) {
      continue;
    }

    const candidateRect = element.getBoundingClientRect();
    const target = center(candidateRect);

    const dx = target.x - origin.x;
    const dy = target.y - origin.y;

    const length = Math.hypot(dx, dy);

    if (!length) {
      continue;
    }

    const forward = (dx * directionX + dy * directionY) / length;

    if (forward < frustumCos) {
      continue;
    }

    let forwardDistance, sidewaysDistance;

    if (vertical) {
      forwardDistance =
        directionY > 0
          ? candidateRect.top - rect.bottom
          : rect.top - candidateRect.bottom;
      sidewaysDistance = Math.max(
        candidateRect.left - rect.right,
        rect.left - candidateRect.right,
        0,
      );
    } else {
      forwardDistance =
        directionX > 0
          ? candidateRect.left - rect.right
          : rect.left - candidateRect.right;
      sidewaysDistance = Math.max(
        candidateRect.top - rect.bottom,
        rect.top - candidateRect.bottom,
        0,
      );
    }

    forwardDistance = Math.max(0, forwardDistance);

    candidates.push({
      element: element,
      score: forwardDistance + sidewaysDistance * 2,
    });
  }

  if (!candidates.length) return;

  candidates.sort((a, b) => a.score - b.score);

  focus(candidates[0].element);
}

function buttonPressed(index) {
  return !!activeGamepad?.buttons[index]?.pressed;
}

function allButtonsReleased() {
  return (
    !activeGamepad || activeGamepad.buttons.every((button) => !button.pressed)
  );
}

function justPressed(index) {
  if (inputLocked) {
    return false;
  }

  const current = buttonPressed(index);
  const notPrevious = !previousButtons[index];

  return current && notPrevious;
}

function getStickDirection(x, y) {
  if (stickDirection) {
    const stillHeld =
      stickDirection === "left"
        ? x < -stickRelease
        : stickDirection === "right"
          ? x > stickRelease
          : stickDirection === "up"
            ? y < -stickRelease
            : stickDirection === "down"
              ? y > stickRelease
              : false;

    if (stillHeld) {
      return stickDirection;
    }

    stickDirection = null;
  }

  if (Math.abs(x) < stickStart && Math.abs(y) < stickStart) {
    return null;
  }

  if (Math.abs(x) > Math.abs(y)) {
    stickDirection = x < 0 ? "left" : "right";
  } else {
    stickDirection = y < 0 ? "up" : "down";
  }

  return stickDirection;
}

function getDirection() {
  if (!activeGamepad) {
    return null;
  }

  const buttons = activeGamepad.buttons;

  if (buttons[12] && buttons[12].pressed) {
    return "up";
  }
  if (buttons[13] && buttons[13].pressed) {
    return "down";
  }
  if (buttons[14] && buttons[14].pressed) {
    return "left";
  }
  if (buttons[15] && buttons[15].pressed) {
    return "right";
  }

  const x = activeGamepad.axes[0] ?? 0;
  const y = activeGamepad.axes[1] ?? 0;

  return getStickDirection(x, y);
}

function processDirection(now) {
  const nextDirection = getDirection();

  if (!nextDirection) {
    direction = null;
    return;
  }

  if (nextDirection !== direction) {
    direction = nextDirection;
    directionStarted = now;
    lastRepeat = now;

    navigate(direction);
    return;
  }

  if (now - directionStarted < initialRepeatDelay) {
    return;
  }

  if (now - lastRepeat >= repeatInterval) {
    lastRepeat = now;
    navigate(direction);
  }
}

function applyDeadzone(value, deadzone) {
  if (Math.abs(value) < deadzone) return 0;

  const sign = Math.sign(value);
  const magnitude = (Math.abs(value) - deadzone) / (1 - deadzone);

  return sign * magnitude;
}

function updateScroll(gamepad) {
  let x = applyDeadzone(gamepad.axes[2] ?? 0, scrollDeadzone);
  let y = applyDeadzone(gamepad.axes[3] ?? 0, scrollDeadzone);

  if (!x && !y) {
    return;
  }

  hideCursor();

  x = Math.sign(x) * x * x;
  y = Math.sign(y) * y * y;

  window.scrollBy({
    left: x * scrollSpeed,
    top: y * scrollSpeed,
  });
}

function update(now) {
  const pads = window.navigator.getGamepads();

  if (activeGamepad) {
    const current = pads[activeGamepad.index];

    if (current) {
      activeGamepad = current;
    }
  }

  if (activeGamepad) {
    updateScroll(activeGamepad);

    if (inputLocked) {
      if (allButtonsReleased()) {
        inputLocked = false;

        previousButtons = activeGamepad.buttons.map((button) => button.pressed);
      }

      window.requestAnimationFrame(update);
      return;
    }

    processDirection(now);

    if (justPressed(0)) {
      hideCursor();

      const current = document.activeElement;

      if (current instanceof HTMLSelectElement) {
        if (selectEditing) {
          submitSelect(current);
        } else {
          enterSelect(current);
        }
      } else {
        inputLocked = true;
        current.click();
      }
    }

    if (justPressed(1)) {
      hideCursor();

      const current = document.activeElement;

      if (selectEditing && current instanceof HTMLSelectElement) {
        cancelSelect(current);
      } else {
        inputLocked = true;
        window.history.back();
      }
    }

    previousButtons = activeGamepad.buttons.map((button) => button.pressed);
  }

  window.requestAnimationFrame(update);
}

window.addEventListener("gamepadconnected", (event) => {
  activeGamepad = event.gamepad;

  console.log("gamepad connected:", activeGamepad["id"]);

  const first = getElements()[0];
  const active = document.activeElement;

  if (first && !(active && active.matches(selector))) {
    focus(first);
  }
});

window.addEventListener("gamepaddisconnected", (event) => {
  if (activeGamepad && activeGamepad.index && event.gamepad.index) {
    activeGamepad = null;
    direction = null;
    stickDirection = null;
    previousButtons = [];
  }
});

document
  .getElementById("gamepad-overlay")
  .addEventListener("mousemove", showCursor);

window.requestAnimationFrame(update);
