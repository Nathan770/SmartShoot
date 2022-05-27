# import the necessary packages
import base64
import io
from typing import Dict, Any, Tuple

import cv2
import numpy as np
from PIL import Image


class FrameProcess:

    def __init__(self) -> None:
        # define the lower and upper boundaries of some colors in the HSV color space - (H- 0-179, S- 0-255, V- 0-255)
        self.COLORS = {
            'black': ((0, 0, 0), (180, 255, 30)),
            'blue': ((100, 150, 0), (140, 255, 255)),
            'green': ((50, 100, 50), (70, 255, 255)),
            'orange': ((0, 120, 120), (10, 255, 255)),
            'red': ((160, 50, 50), (180, 255, 255)),
            'white': ((0, 0, 200), (179, 50, 255)),
        }
        self.frame_status = {
            'frame': '', #bytes(),
            'is_hoop_detected': False,
            'is_ball_detected': False,
            'is_inside_hoop_box': False,
            'is_obj_in_frame': False,
            'is_shot': False,
            'is_3': False,
            'is_2': False,
            'is_1': False
        }
        self.status = 0
        self.com_frame = None
        self.is_first_frame = True
        self.hoop_box = (-1, -1, -1, -1)
        self.net_box = (-1, -1, -1, -1)
        self.hoop_tracker = cv2.TrackerKCF_create()
        self.shoot_box_tracker = cv2.TrackerKCF_create()
        self.is_tracker_init = False
        self.num_of_shots = 0

    def highlights(self, frame, color='orange') -> np.ndarray:
        try:
            self.frame_status['is_shot'] = False
            if frame is None:
                raise ValueError('Problem with frame provided')
            # check if tracker has been initialized
            if not self.is_tracker_init:
                self.tracker_init(frame)
            frame = self.tracker_update(frame)
            masked_frame = self.manipulate_frame(frame, self.COLORS[color][0], self.COLORS[color][1])
            edged_frame = cv2.Canny(masked_frame, 30, 150)
            frame, ball_x, ball_y, ball_radius = self.detect_ball(edged_frame, frame)
            frame = self.detect_shot(ball_x, ball_y, ball_radius, frame)
            # self.frame_status['frame'] = self.convert_to_byte_array(frame)
            self.frame_status['frame'] = self.convert_to_str(frame)
            return frame
        except Exception:
            self.status = -1
            return frame

    def detect_ball(self, p_frame: np.ndarray, main_frame: np.ndarray) -> (np.ndarray, int, int, int):
        x, y, radius = -1, -1, -1
        if self.is_first_frame:
            self.com_frame = p_frame
            self.is_first_frame = False
        else:
            # make a diff frame between two consecutive frames
            diff_frame = cv2.absdiff(self.com_frame, p_frame)
            # find contours
            cnts = cv2.findContours(diff_frame, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)[0]
            # only proceed if at least one contour was found
            if len(cnts) > 0:
                # most likely the ball is the largest contour
                c = max(cnts, key=cv2.contourArea)
                # compute the minimum enclosing circle
                (x, y), radius = cv2.minEnclosingCircle(c)
                radius = 10 if radius < 10 else radius
                self.draw_circle(main_frame, (int(x), int(y)), int(radius), (0, 255, 255), 2)
                self.frame_status['is_ball_detected'] = True
            else:
                self.frame_status['is_ball_detected'] = False
            self.com_frame = p_frame
        return main_frame, x, y, radius

    def detect_hoop(self, frame: np.ndarray) -> bool:
        # crop frame to the higher fifth of frame to search for hoop
        cropped_frame = frame[:frame.shape[1] // 5, :]
        cropped_frame = self.manipulate_frame(cropped_frame, self.COLORS['orange'][0], self.COLORS['orange'][1])
        # find contours
        contours = cv2.findContours(cropped_frame.copy(), cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)[0]
        if len(contours) > 0:
            # most likely the hoop is the largest contour
            required_contour = max(contours, key=cv2.contourArea)
            x, y, w, h = cv2.boundingRect(required_contour)
            self.hoop_box = (x, y, w, h)
            self.net_box = (x + (w // 6), y + h, 2 * w // 3, w)
            self.draw_rec(frame, (x, y), (x + w, y + h), (255, 0, 0), 2)
            print(f'hoop_box: {x, y, w, h}')
            print(f'shoot_box: {x + (w // 6), y + h, 2 * w // 3, w}')
            # self.frame_status['frame'] = self.convert_to_byte_array(frame)
            self.frame_status['frame'] = self.convert_to_str(frame)
            self.frame_status['is_hoop_detected'] = True
            return True
        self.frame_status['frame'] = self.convert_to_str(frame)
        return False

    def detect_shot(self, x: int, y: int, radius: int, main_frame: np.ndarray) -> np.ndarray:
        offset = 5 if radius >= 10 else 20
        if self.is_inside_box(x, y, radius, self.net_box, offset) and self.frame_status['is_inside_hoop_box']:
            self.frame_status['is_shot'] = True
            self.status = 1
            # color hoop & net box in green
            self.draw_rec(main_frame, (self.hoop_box[0], self.hoop_box[1]), (self.hoop_box[2], self.hoop_box[3]),
                          (0, 255, 0), 2)
            self.draw_rec(main_frame, (self.net_box[0], self.net_box[1]), (self.net_box[2], self.net_box[3]),
                          (0, 255, 0), 2)
            self.num_of_shots += 1
        else:
            self.status = 0
        self.frame_status['is_inside_hoop_box'] = self.is_inside_box(x, y, radius, self.hoop_box)
        return main_frame

    def spotlight(self, frame: np.ndarray, color: str) -> np.ndarray:
        try:
            color_lower, color_upper = self.COLORS[color.lower()]
            if frame is None:
                print('frame could not be converted!')
                raise ValueError('Problem with frame provided')
            masked_frame = self.manipulate_frame(frame, color_lower, color_upper)
            # find contours in the mask
            cnts = cv2.findContours(masked_frame.copy(), cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)[0]
            # only proceed if at least one contour was found
            if len(cnts) > 0:
                # find the largest contour in the mask, then use it to compute the bounding rectangle around player
                c = max(cnts, key=cv2.contourArea)
                x, y, w, h = cv2.boundingRect(c)
                # draw the circle on the frame
                self.draw_rec(frame, (x, y), (x + w, y + h), (0, 255, 255), 2)
                self.frame_status['is_obj_in_frame'] = True
                self.status = 1
            else:
                self.frame_status['is_obj_in_frame'] = False
                self.status = 0
            # self.frame_status['frame'] = self.convert_to_byte_array(frame)
            self.frame_status['frame'] = self.convert_to_str(frame)
            return frame
        except Exception:
            self.status = -1
            return frame

    def tracker_init(self, frame: np.ndarray) -> None:
        self.hoop_tracker.init(frame, self.hoop_box)
        self.shoot_box_tracker.init(frame, self.net_box)
        self.is_tracker_init = True

    def tracker_update(self, frame: np.ndarray) -> np.ndarray:
        # grab the new bounding box coordinates of the object
        success_hoop, h_box = self.hoop_tracker.update(frame)
        success_net, n_box = self.shoot_box_tracker.update(frame)
        # check to see if the tracking was a success
        if success_hoop:
            frame = self.draw_box(h_box, frame, 'hoop')
            self.frame_status['is_hoop_detected'] = True
        else:
            self.frame_status['is_hoop_detected'] = False
        if success_net:
            frame = self.draw_box(n_box, frame, 'net')
        return frame

    def draw_box(self, box: Tuple[int, int, int, int], frame: np.ndarray, obj: str) -> np.ndarray:
        x, y, w, h = box
        self.draw_rec(frame, (x, y), (x + w, y + h), (255, 0, 0), 2)
        if obj == 'hoop':
            self.hoop_box = x, y, x + w, y + h
        else:
            self.net_box = x, y, x + w, y + h
        return frame

    def manipulate_frame(self, frame: np.ndarray, lower: Tuple[int, int, int],
                         upper: Tuple[int, int, int]) -> np.ndarray:
        # blur the frame and convert it to the HSV color space
        blurred = cv2.GaussianBlur(frame, (11, 11), 0)
        hsv = cv2.cvtColor(blurred, cv2.COLOR_BGR2HSV)
        # construct a mask for the selected color, then perform
        # a series of dilations and erosions to remove any small blobs left in the mask
        mask = cv2.inRange(hsv, lower, upper)
        mask = cv2.erode(mask, None, iterations=2)
        mask = cv2.dilate(mask, None, iterations=2)
        # cv2.imshow('mask', mask)
        return mask

    def draw_rec(self, frame: np.ndarray, p1: Tuple[int, int], p2: Tuple[int, int], color: Tuple[int, int, int],
                 thickness: int) -> None:
        cv2.rectangle(frame, p1, p2, color, thickness)

    def draw_circle(self, frame: np.ndarray, center: Tuple[int, int], radius: int, color: Tuple[int, int, int],
                    thickness: int) -> None:
        cv2.circle(frame, center, radius, color, thickness)

    def is_inside_box(self, x: int, y: int, radius: int, box: Tuple[int, int, int, int], offset=5) -> bool:
        return box[0] - offset < x < box[2] + offset \
               and box[1] - offset < y < box[3] + offset \
               and (box[2] - box[0]) // 4 <= radius <= box[2] - box[0]

    def convert_from_byte_array(self, byte_array: bytes) -> np.ndarray:
        frame = Image.open(io.BytesIO(bytes(byte_array)))
        frame = np.array(frame)
        # frame = frame[:, :, ::-1].copy()  # Convert RGB to BGR
        return frame

    def convert_to_byte_array(self, frame: np.ndarray) -> bytes:
        success, im_buf_arr = cv2.imencode('.png', frame)
        return im_buf_arr.tobytes() if success else None

    def get_status(self) -> Dict[str, Any]:
        return self.frame_status

    def convert_from_str(self, frame):
        decoded_frame = base64.b64decode(frame)
        frame_np = np.fromstring(decoded_frame, np.uint8)
        frame = cv2.imdecode(frame_np, cv2.IMREAD_UNCHANGED)
        return frame

    def convert_to_str(self, frame):
        pil_frame = Image.fromarray(frame)
        buff = io.BytesIO()
        pil_frame.save(buff, format='PNG')
        frame_str = base64.b64encode(buff.getvalue())
        return '' + str(frame_str, 'utf-8')