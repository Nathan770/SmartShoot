from FrameProcess import FrameProcess

game = FrameProcess()


def init():
    global game
    game = FrameProcess()


def detect_hoop(byte_array_frame) -> int:
    frame = game.convert_from_byte_array(byte_array_frame)
    status = game.detect_hoop(frame)
    return 1 if status else 0


def highlights(byte_array_frame, color='orange') -> bytes:
    frame = game.convert_from_byte_array(byte_array_frame)
    frame = game.highlights(frame, color.lower())
    return game.convert_to_byte_array(frame)


def spotlight(byte_array_frame, color: str):
    frame = game.convert_from_byte_array(byte_array_frame)
    frame = game.spotlight(frame, color.lower())
    return game.convert_to_byte_array(frame)


def get_status():
    status = game.status
    # status_list = list(reduce(lambda x, y: x + y, status))
    # status_list = [str(element) for element in status_list]
    return status


def get_frame():
    return game.get_status()['frame']


def get_num_of_shots():
    return game.num_of_shots