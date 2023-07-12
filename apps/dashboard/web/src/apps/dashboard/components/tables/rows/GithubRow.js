import {
    IndexTable,
    Text,
    Badge,
    VerticalStack,
    HorizontalStack,
    Icon,
    Box,
    Button, 
    Popover, 
    ActionList,
    Link
} from '@shopify/polaris';
import {
    HorizontalDotsMinor
} from '@shopify/polaris-icons';
import { useNavigate } from "react-router-dom";
import { useState, useCallback } from 'react';
import TestingStore from '../../../pages/testing/testingStore';
import './row.css'
import GithubCell from '../cells/GithubCell';

function GithubRow(props) {
    const navigate = useNavigate();
    const [popoverActive, setPopoverActive] = useState(-1);
    const togglePopoverActive = (index) =>useCallback(
        () => setPopoverActive(index),
        [],
    );
    
    const selectedTestRun = TestingStore(state => state.selectedTestRun)
    const setSelectedTestRun = TestingStore(state => state.setSelectedTestRun)
    const setSelectedTestRunResult = TestingStore(state => state.setSelectedTestRunResult)
    function nextPage(data, page){
        switch(page){
            case 1: 
                setSelectedTestRun(data)
                navigate("/dashboard/testing/"+data.hexId)
                break;
            case 2:
                setSelectedTestRunResult(data)
                navigate("/dashboard/testing/"+selectedTestRun.hexId +"/result/" + data.hexId)
                break;
            default:
                break;            
        }
    }

    const [rowClickable, setRowClickable] = useState(props.page==2)

    return (
        <IndexTable.Row
            id={props.data.hexId}
            key={props.data.hexId}
            selected={props.selectedResources.includes(props.data.hexId)}
            position={props.index}
        >
                {/* <div style={{ padding: '12px 16px', width: '100%' }}> */}
                {/* <HorizontalStack align='space-between'> */}
            <IndexTable.Cell>
                {/* <div onClick={() => (props.nextPage && props.nextPage=='singleTestRunPage' ? navigateToTest(props.data) : {})} style={{cursor: 'pointer'}}> */}
                <div className='linkClass'>
                <Link
                    {...(rowClickable ? {dataPrimaryLink: rowClickable} : {})}
                    monochrome
                    removeUnderline
                    onClick={() => (nextPage(props.data, props.page))}
                    // onClick={() => console.log("something")}
                >
                    <GithubCell
                        headers = {props.headers}
                        data = {props.data}
                    />
                        </Link>
                        </div>
                    {/* </div> */}
                        </IndexTable.Cell>
                        <IndexTable.Cell>
                    <VerticalStack align="center" inlineAlign="center">
                    {
                        props.hasRowActions &&
                        <Popover
                            active={popoverActive == props.data.hexId}
                            activator={<Button onClick={togglePopoverActive(props.data.hexId)} plain icon={HorizontalDotsMinor} />}
                            autofocusTarget="first-node"
                            onClose={togglePopoverActive(popoverActive)}
                        >
                            <ActionList
                                actionRole="menuitem"
                                sections={props.getActions(props.data)}
                            />
                        </Popover>
                    }
                    </VerticalStack>
                {/* </HorizontalStack> */}
                {/* </div> */}
            </IndexTable.Cell>
        </IndexTable.Row>
    )

}

export default GithubRow;