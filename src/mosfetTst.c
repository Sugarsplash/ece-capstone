#include <avr/io.h>
#include <util/delay.h>

int main()
{
	DDRC = (1<<0);
	PORTC = (1<<0);
	
	while(1)
	{
		_delay_ms(10000);
		PORTC = (0<<0);
		_delay_ms(10000);
		PORTC = (1<<0);
	}
}

