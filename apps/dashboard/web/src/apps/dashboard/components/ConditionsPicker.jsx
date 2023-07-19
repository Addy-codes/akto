import { Button, ButtonGroup, LegacyCard, TextField, VerticalStack } from '@shopify/polaris'
import React, { useEffect, useState } from 'react'
import Dropdown from './layouts/Dropdown';
import {DeleteMinor} from "@shopify/polaris-icons"

function ConditionsPicker(props) {

    const {title, param, items, initialItems, conditionOp, fetchChanges, setChange} = props
    const [condition, setCondition] = useState('')
    const [textFields, setTextFields] = useState([]);

    useEffect(()=>{
        setTextFields(initialItems)
        setCondition(conditionOp)
    },[initialItems,conditionOp])

    const handleChange = (value, index) => {
        const updatedFields = [...textFields];
        updatedFields[index].value = value;
        let obj = {
            predicates: updatedFields,
            operator: condition,
        }
        fetchChanges(obj)
        setChange(true)
        setTextFields(updatedFields);
    };

    const handleDelete = (index) => {
        const updatedFields = [...textFields];
        updatedFields.splice(index, 1);
        let obj = {
            predicates: updatedFields,
            operator: condition,
        }
        fetchChanges(obj)
        setChange(true)
        setTextFields(updatedFields);
    };

    const handleAddTextField = () => {
        const updatedFields = [...textFields, { type: items[0].value, value: '', }];
        setTextFields(updatedFields);
    };

    const orAndConditions = [
        {
            label: 'OR',
            value: 'OR',
        },
        {
            label: 'AND',
            value: 'AND'
        }
    ]

    const handleConditionSelected = (val) =>{
        let obj = {
            predicates: textFields,
            operator: val,
        }
        fetchChanges(obj)
        setCondition(val)
    }
    const handleRegexSelected = (value,index) =>{
        const updatedFields = [...textFields];
        updatedFields[index].type = value;
        let obj = {
            predicates: updatedFields,
            operator: condition,
        }
        fetchChanges(obj)
        setTextFields(updatedFields);
    }

    const prefixLeft= (index) =>(
        <ButtonGroup>
            {index > 0 ? (<Dropdown menuItems={orAndConditions} initial={condition} selected={handleConditionSelected}/>) : null}
            <TextField value={param} />
            <Dropdown menuItems={items} initial={textFields[index].type} selected={(val) => handleRegexSelected(val,index)}/>
        </ButtonGroup>
    )

    const textFieldsComponent = (
        <VerticalStack gap="4">
            {textFields.length > 0 && textFields.map((field,index)=> (
                <TextField connectedLeft={prefixLeft(index)} connectedRight={<Button icon={DeleteMinor} onClick={() => handleDelete(index)} />} 
                            value={field.value}
                            onChange={(newValue) => handleChange(newValue, index)}
                            key={index}
                />
            ))}
        </VerticalStack>
    )

    return (
        <LegacyCard.Section title={title}>
            {textFieldsComponent}
            <br/>
            <Button onClick={handleAddTextField}>Add Condition</Button>
        </LegacyCard.Section>
    )
}

export default ConditionsPicker