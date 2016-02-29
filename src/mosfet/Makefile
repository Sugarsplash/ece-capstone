NAME=main
FILES=mosfetTst.c

MCU=atmega328p
PROGRAMMER=usbtiny
CPU=1000000
BAUD=9600

CC=avr-gcc
OBJCOPY=avr-objcopy

CFLAGS=-Wall -Os -DBAUD=${BAUD} -DF_CPU=${CPU} -mmcu=${MCU}
PROGFLAGS=-p ${MCU} -c ${PROGRAMMER}

${NAME}.hex: ${NAME}.elf
	${OBJCOPY} -O ihex $< $@

${NAME}.elf: ${FILES}
	${CC} ${CFLAGS} -o $@ $^

install: ${NAME}.hex
	avrdude ${PROGFLAGS} -e -U flash:w:$<

clean:
	rm -rf *.elf *.hex