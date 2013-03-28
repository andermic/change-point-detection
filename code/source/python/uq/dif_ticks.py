from datetime import datetime

def dif_ticks(subtrahend, minuend):
    subt_dt = datetime.strptime(subtrahend[:-4], '%d/%m/%Y %H:%M:%S')
    minu_dt = datetime.strptime(minuend[:-4], '%d/%m/%Y %H:%M:%S')
    subt_milli = subtrahend[-4:]
    minu_milli = minuend[-4:]

    delta = subt_dt - minu_dt
    result = (delta.days * 24 * 3600 + delta.seconds) * 30
    result += int(round((float(subt_milli) - float(minu_milli)) * 30))
    return result
